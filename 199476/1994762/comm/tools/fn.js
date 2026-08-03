/**
 * 将数组对象存入 localStorage
 * @param {Array<Object>} dataArray - 需要存储的数组对象
 * @param {String<String>} prefixStr - 前缀
 */
function saveToLocal (dataArray, prefixStr = "page_") {
  // 1. 参数校验：确保传入的是数组
  if (!Array.isArray(dataArray)) {
    console.error('saveToLocal Error: 传入的参数必须是一个数组');
    return;
  }

  // 2. 生成 Key：格式为 page_年月日_毫秒
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0'); // 月份补零
  const day = String(now.getDate()).padStart(2, '0');        // 日期补零
  const milliseconds = now.getTime();                // 毫秒数

  const key = `${prefixStr}${year}${month}${day}_${milliseconds}`;

  // 3. 存储数据：将数组对象序列化为 JSON 字符串
  try {
    localStorage.setItem(key, JSON.stringify(dataArray));
    console.log(`数据保存成功，Key: ${key}`);
  } catch (error) {
    console.error('localStorage 存储失败:', error);
  }
}

// --- 使用示例 ---
// saveToLocal(data,"page_")


/**
 * 解析的网址是：https://www.cwl.gov.cn/ygkj/ssq/ydjzjmx/
 * 解析包含 rowspan 的 HTML Table 并返回 JSON 数据
 * @param {HTMLTableElement} table - 目标表格 DOM 元素
 * @returns {Array<Object>} 解析后的 JSON 数组
 */
function parseTableToJson (table) {
  const rows = Array.from(table.querySelectorAll('tbody tr'));
  const headers = Array.from(table.querySelectorAll('thead tr td, thead tr th')).map(th => th.innerText.trim());

  const result = [];
  // 记录每一行每一列是否被占用，以及占用的值
  const occupied = {};

  rows.forEach((tr, rowIndex) => {
    const rowData = {};
    let colIndex = 0;
    const cells = Array.from(tr.children);

    cells.forEach(cell => {
      // 寻找当前行下一个未被占用的列
      while (occupied[rowIndex] && occupied[rowIndex][colIndex]) {
        rowData[headers[colIndex] || `col_${colIndex}`] = occupied[rowIndex][colIndex];
        colIndex++;
      }

      const header = headers[colIndex] || `col_${colIndex}`;
      const rowspan = parseInt(cell.getAttribute('rowspan')) || 1;
      const value = cell.innerText.trim();

      rowData[header] = value;

      // 如果存在 rowspan，将值缓存到后续行的对应列中
      if (rowspan > 1) {
        for (let i = 1; i < rowspan; i++) {
          if (!occupied[rowIndex + i]) occupied[rowIndex + i] = {};
          occupied[rowIndex + i][colIndex] = value;
        }
      }
      colIndex++;
    });

    // 处理行末尾可能遗留的被占用列
    while (occupied[rowIndex] && occupied[rowIndex][colIndex]) {
      rowData[headers[colIndex] || `col_${colIndex}`] = occupied[rowIndex][colIndex];
      colIndex++;
    }

    result.push(rowData);
  });

  return result;
}

// --- 使用示例 ---
// const table = document.querySelector('.bgzt table');
// if (table) {
//   const jsonData = parseTableToJson(table);
//   console.log(jsonData);
// }

/**
 * 解析的网址是：https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/
 * 解析双色球开奖表格并返回结构化的 JSON 数据
 * @param {HTMLTableElement} table - 目标表格 DOM 元素
 * @returns {Array<Object>} 解析后的 JSON 数组
 */
function parseSsqTableToJson (table) {
  const rows = Array.from(table.querySelectorAll('tbody tr'));
  const result = [];

  rows.forEach(tr => {
    const cells = Array.from(tr.children);

    // 1. 提取并处理“开奖号码”
    const ballElements = cells[2].querySelectorAll('.qiu-item');
    const allNumbers = Array.from(ballElements).map(ball => ball.innerText.trim());

    // 2. 拆分红球和蓝球（最后一个永远是蓝球，其余是红球）
    const blueBall = allNumbers.pop();
    const redBalls = allNumbers;

    // 3. 构建结构化对象
    const rowData = {
      "期号": cells[0].innerText.trim(),
      "开奖日期": cells[1].innerText.trim(),
      "红球": redBalls.join(','),
      "蓝球": blueBall,
      "一等奖注数": cells[3].innerText.trim(),
      "一等奖金额": cells[4].innerText.trim(),
      "二等奖注数": cells[5].innerText.trim(),
      "二等奖金额": cells[6].innerText.trim(),
      "销售额（元）": cells[7].innerText.trim(),
      "奖池金额（元）": cells[8].innerText.trim()
    };

    result.push(rowData);
  });

  return result;
}

// --- 使用示例 ---
// const tableElement = document.querySelector('table.ssq_table');
// if (tableElement) {
// const jsonData = parseSsqTableToJson(tableElement);
// console.log(jsonData);
// }


/**
 * 原封不动导出 localStorage 源数据到 TXT 文件
 * @Params perFix 前缀
 */
async function exportRawDataToTxt (perFix = "page_") {
  let allData = [];
  console.log('开始扫描并合并 localStorage...');

  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key.startsWith(perFix)) {
      try {
        const dataArray = JSON.parse(localStorage.getItem(key));
        if (Array.isArray(dataArray) && dataArray.length > 0) {
          allData = allData.concat(dataArray);
          console.log(`✅ 合并数据: ${key} (当前总条数: ${allData.length})`);
        }
      } catch (e) {
        console.warn(`❌ 解析失败，已跳过: ${key}`);
      }
    }
  }

  if (allData.length === 0) {
    console.error(`⚠️ 未找到任何以 "${perFix}" 开头且包含有效数组数据的缓存。`);
    return;
  }

  // 原封不动地将整个大数组转换为 JSON 字符串
  const txtContent = JSON.stringify(allData, null, 2);

  // 触发下载
  const blob = new Blob([txtContent], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'Raw_Data_Backup.txt';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  console.log(`🎉 TXT 导出成功！已将 ${allData.length} 条原始数据原样打包。`);
}

// --- 调用示例 ---
// exportRawDataToTxt("page_");


/**
 * 导出 localStorage 数据到 Excel（支持自定义排序配置版）
 * @param {string} perFix - localStorage 键的前缀
 * @param {number} maxRowsPerSheet - 单个 Sheet 允许容纳的最大数据行数
 * @param {Object|string} sortConfig - 排序配置对象 {sortKey: '字段名', sortType: 1} 或直接传字段名字符串
 */
async function exportLocalStorageToExcel (perFix = "page_", maxRowsPerSheet = 2000, sortConfig = '开奖日期') {
  // 1. 处理排序参数（兼容旧写法和新对象写法）
  let sortKey = '开奖日期';
  let sortType = 1; // 1: 日期排序, 2: 数字排序

  if (typeof sortConfig === 'object') {
    sortKey = sortConfig.sortKey || '开奖日期';
    sortType = sortConfig.sortType || 1;
  } else {
    sortKey = sortConfig;
  }

  // 2. 动态加载 xlsx 库
  if (!window.XLSX) {
    console.log('正在加载 xlsx 库...');
    await new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/xlsx/dist/xlsx.full.min.js';
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
    console.log('xlsx 库加载成功！');
  }

  // 3. 收集并合并所有 ${perFix} 开头的有效数据
  let allData = [];
  let headers = null;
  console.log('开始扫描并合并 localStorage...');

  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key.startsWith(perFix)) {
      try {
        const value = localStorage.getItem(key);
        const dataArray = JSON.parse(value);
        if (Array.isArray(dataArray) && dataArray.length > 0) {
          if (!headers) headers = Object.keys(dataArray[0]);
          allData = allData.concat(dataArray);
          console.log(`✅ 合并数据: ${key} (当前总条数: ${allData.length})`);
        }
      } catch (e) {
        console.warn(`❌ 解析失败，已跳过: ${key}`);
      }
    }
  }

  if (allData.length === 0 || !headers) {
    console.error(`⚠️ 未找到任何以 "${perFix}" 开头且包含有效数组数据的缓存。`);
    return;
  }

  // 4. 核心优化：处理中文日期格式（如 "2023年5月30日"）
  function parseChineseDate (dateStr) {
    if (!dateStr) return 0;
    const match = String(dateStr).match(/(\d{4})年(\d{1,2})月(\d{1,2})日/);
    if (match) {
      const standardDate = `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`;
      return new Date(standardDate).getTime();
    }
    return new Date(dateStr).getTime() || 0;
  }

  // 5. 按指定字段和类型排序
  if (headers.includes(sortKey)) {
    console.log(`🗓️ 正在按字段 [${sortKey}] 排序 (类型: ${sortType === 1 ? '日期' : '数字'})...`);

    allData.sort((a, b) => {
      const valA = a[sortKey];
      const valB = b[sortKey];

      if (sortType === 1) {
        // 日期排序
        return parseChineseDate(valA) - parseChineseDate(valB);
      } else {
        // 数字排序 (处理带逗号的数字字符串，如 "1,000")
        const numA = parseFloat(String(valA).replace(/,/g, '')) || 0;
        const numB = parseFloat(String(valB).replace(/,/g, '')) || 0;
        return numA - numB;
      }
    });
  } else {
    console.warn(`⚠️ 数据中未找到字段 [${sortKey}]，将按原始顺序导出。当前可用字段: ${headers.join(', ')}`);
  }

  // 6. 按 maxRowsPerSheet 切分 Sheet
  const wb = XLSX.utils.book_new();
  let sheetIndex = 1;

  while (allData.length > 0) {
    const chunk = allData.splice(0, maxRowsPerSheet);
    const wsData = [headers, ...chunk.map(row => headers.map(h => row[h] ?? ''))];
    const ws = XLSX.utils.aoa_to_sheet(wsData);
    XLSX.utils.book_append_sheet(wb, ws, `Sheet_${sheetIndex}`);
    sheetIndex++;
  }

  // 7. 触发下载
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
  const blob = new Blob([wbout], { type: 'application/octet-stream' });
  const url = URL.createObjectURL(blob);

  const a = document.createElement('a');
  a.href = url;
  a.download = `Merged_Export_${sortKey}.xlsx`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  console.log(`🎉 导出成功！共生成 ${sheetIndex - 1} 个 Sheet，数据已按 [${sortKey}] 排序。`);
}
// --- 调用示例 ---
//exportLocalStorageToExcel("page_ssq_", 2000, { sortKey: "期号", sortType: 2 });


/**
 * 定时循环抓取数据 https://www.cwl.gov.cn/ygkj/ssq/ydjzjmx/ 这个网址下的数据
 * @param {number} maxRuns - 最多运行次数
 * @param {number} interval - 每次运行的间隔时间（毫秒）
 * @param {string} perFix - 前缀
 */
async function startAutoScrape (maxRuns = 10, interval = 5000, perFix = "page_") {
  let runCount = 0;

  console.log(`🚀 自动抓取任务已启动！目标运行次数: ${maxRuns}，间隔: ${interval / 1000}秒`);

  while (runCount < maxRuns) {
    runCount++;
    console.log(`⏳ 正在执行第 ${runCount}/${maxRuns} 次抓取...`);

    try {
      // 1. 模拟点击下一页
      const nextBtn = document.querySelector("body > div.containerHome > div > div > div > div.pagebar > ul > li:nth-child(11) > a");
      if (nextBtn) {
        nextBtn.click();
      } else {
        console.warn(`⚠️ 第 ${runCount} 次：未找到下一页按钮，可能已到达最后一页。`);
      }

      // 2. 等待页面加载（这里简单等待1秒，您可以根据实际网速调整）
      await new Promise(resolve => setTimeout(resolve, 1000));

      // 3. 获取表格数据并保存
      const table = document.querySelector('.bgzt table');
      if (table) {
        // 确保 parseTableToJson 和 saveToLocal 函数在当前环境中可用
        if (typeof parseTableToJson === 'function' && typeof saveToLocal === 'function') {
          const jsonData = parseTableToJson(table);
          saveToLocal(jsonData, perFix);
          console.log(`✅ 第 ${runCount} 次：抓取并保存成功，获取 ${jsonData.length} 条数据。`);
        } else {
          console.error('❌ 缺少必要函数：请确保 parseTableToJson 和 saveToLocal 已定义！');
        }
      } else {
        console.warn(`⚠️ 第 ${runCount} 次：未找到目标表格 (.bgzt table)。`);
      }

    } catch (error) {
      console.error(`❌ 第 ${runCount} 次抓取发生异常:`, error);
    }

    // 4. 如果还没达到最大次数，等待设定的间隔时间
    if (runCount < maxRuns) {
      console.log(`💤 等待 ${interval / 1000} 秒后进行下一次...`);
      await new Promise(resolve => setTimeout(resolve, interval));
    }
  }

  console.log('🎉 所有抓取任务已完成！');
}

// --- 调用示例：每隔 5 秒运行一次，总共运行 5 次 ---
// startAutoScrape(5, 5000, "page_");



/**
 * 定时循环抓取数据 https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/ 这个网址下的数据
 * @param {number} maxRuns - 最多运行次数
 * @param {number} interval - 每次运行的间隔时间（毫秒）
 * @param {string} perFix - 前缀
 * 
 */
async function startAutoScrapeSSQ (maxRuns = 10, interval = 5000, perFix = "page_ssq_") {
  let runCount = 0;

  console.log(`🚀 自动抓取任务已启动！目标运行次数: ${maxRuns}，间隔: ${interval / 1000}秒`);

  while (runCount < maxRuns) {
    runCount++;
    console.log(`⏳ 正在执行第 ${runCount}/${maxRuns} 次抓取...`);

    try {
      // 1. 模拟点击下一页
      const nextBtn = document.querySelector("#layui-laypage-1 > a.layui-laypage-next");
      if (nextBtn) {
        nextBtn.click();
      } else {
        console.warn(`⚠️ 第 ${runCount} 次：未找到下一页按钮，可能已到达最后一页。`);
      }

      // 2. 等待页面加载（这里简单等待1秒，您可以根据实际网速调整）
      await new Promise(resolve => setTimeout(resolve, 1000));


      // 3. 获取表格数据并保存
      const table = document.querySelector('table.ssq_table');
      if (table) {
        // 确保 parseSsqTableToJson 和 saveToLocal 函数在当前环境中可用
        if (typeof parseSsqTableToJson === 'function' && typeof saveToLocal === 'function') {
          const jsonData = parseSsqTableToJson(table);
          saveToLocal(jsonData, perFix);
          console.log(`✅ 第 ${runCount} 次：抓取并保存成功，获取 ${jsonData.length} 条数据。`);
        } else {
          console.error('❌ 缺少必要函数：请确保 parseSsqTableToJson 和 saveToLocal 已定义！');
        }
      } else {
        console.warn(`⚠️ 第 ${runCount} 次：未找到目标表格 (.bgzt table)。`);
      }

    } catch (error) {
      console.error(`❌ 第 ${runCount} 次抓取发生异常:`, error);
    }

    // 4. 如果还没达到最大次数，等待设定的间隔时间
    if (runCount < maxRuns) {
      console.log(`💤 等待 ${interval / 1000} 秒后进行下一次...`);
      await new Promise(resolve => setTimeout(resolve, interval));
    }
  }

  console.log('🎉 所有抓取任务已完成！');
}

// --- 调用示例：每隔 5 秒运行一次，总共运行 5 次 ---
// startAutoScrapeSSQ(5, 5000, "page_ssq_");


/**
 *
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 * Busiess
 */


// 定时循环抓取数据 https://www.cwl.gov.cn/ygkj/ssq/ydjzjmx/ 这个网址下的数据
// startAutoScrape(5, 5000, "page_");

// 定时循环抓取数据 https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/ 这个网址下的数据
// startAutoScrapeSSQ(5, 5000, "page_ssq_");

// 导出为 Excel
// exportLocalStorageToExcel("page_ssq_", 2000, { sortKey: "期号", sortType: 2 });

// 导出为 Txt
// exportRawDataToTxt("page_");



