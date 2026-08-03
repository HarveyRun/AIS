package com.diancheng.api;

import com.diancheng.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;

    public FileController(JdbcTemplate jdbc, AuthService auth) { this.jdbc = jdbc; this.auth = auth; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest request, @RequestPart MultipartFile file,
                                      @RequestParam String id, @RequestParam(required = false) String kind) throws Exception {
        String email = auth.requireUser(request);
        if (!"resume".equals(kind)) throw new ApiException(HttpStatus.BAD_REQUEST, "不支持该文件类型。");
        if (file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "请选择需要上传的文件。");
        if (file.getSize() > 20L * 1024 * 1024) throw new ApiException(HttpStatus.BAD_REQUEST, "文件不能超过 20 MB。");
        String fileName = file.getOriginalFilename() == null ? id : file.getOriginalFilename();
        byte[] content = file.getBytes();
        boolean pdfHeader = content.length >= 5 && content[0] == '%' && content[1] == 'P'
                && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
        if (!fileName.toLowerCase().endsWith(".pdf") || !pdfHeader)
            throw new ApiException(HttpStatus.BAD_REQUEST, "简历只支持有效的 PDF 文件。");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM stored_files WHERE id=?", Integer.class, id) > 0)
            throw new ApiException(HttpStatus.CONFLICT, "文件标识已存在，请重新选择文件。");
        jdbc.update("INSERT INTO stored_files(id,user_email,kind,file_name,content_type,size_bytes,content,created_at) VALUES(?,?,?,?,?,?,?,NOW(3))",
                id, email, kind, fileName, MediaType.APPLICATION_PDF_VALUE, file.getSize(), content);
        return Map.of("ok", true);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(HttpServletRequest request, @PathVariable String id,
                                            @RequestParam(required = false) String name) {
        String email = auth.requireUser(request); boolean admin = AuthService.ADMIN_EMAIL.equals(email);
        var rows = jdbc.queryForList("SELECT user_email,file_name,content_type,content FROM stored_files WHERE id=?", id);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();
        Map<String, Object> row = rows.get(0);
        if (!admin && !email.equals(row.get("user_email"))) throw new ApiException(HttpStatus.FORBIDDEN, "没有权限下载该文件。");
        String fileName = name == null || name.isBlank() ? String.valueOf(row.get("file_name")) : name;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"))
                .contentType(MediaType.parseMediaType(String.valueOf(row.get("content_type"))))
                .body((byte[]) row.get("content"));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(HttpServletRequest request, @PathVariable String id) {
        String email = auth.requireUser(request);
        if (AuthService.ADMIN_EMAIL.equals(email)) jdbc.update("DELETE FROM stored_files WHERE id=?", id);
        else jdbc.update("DELETE FROM stored_files WHERE id=? AND user_email=?", id, email);
        return Map.of("ok", true);
    }
}
