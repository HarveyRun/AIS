/* 一次性迁移脚本：从项目 JSON 中移除已废弃字段（总费用区间/总周期区间/评估用条件） */
const fs = require("node:fs");
const path = require("node:path");

const CONTENT_DIR = path.resolve(__dirname, "..", "content", "countries");
const DROP = ["totalEstimate", "totalMonths", "eligibility"];
let touched = 0;

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
    } else if (entry.name.endsWith(".json")) {
      const data = JSON.parse(fs.readFileSync(full, "utf-8"));
      let changed = false;
      const clean = (obj) => {
        if (Array.isArray(obj)) return obj.forEach(clean);
        if (obj && typeof obj === "object") {
          for (const key of Object.keys(obj)) {
            if (DROP.includes(key)) {
              delete obj[key];
              changed = true;
            } else {
              clean(obj[key]);
            }
          }
        }
      };
      clean(data);
      if (changed) {
        fs.writeFileSync(full, JSON.stringify(data, null, 2) + "\n", "utf-8");
        touched++;
        console.log("cleaned:", path.relative(CONTENT_DIR, full));
      }
    }
  }
}

walk(CONTENT_DIR);
console.log(`\n共清洗 ${touched} 个项目文件`);
