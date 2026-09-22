import { checkContent } from "../src/lib/content";

/**
 * 内容体检脚本：npm run content:check
 * 校验全部内容文件（schema + 交叉引用），打印统计与问题清单。
 */
const result = checkContent();

if (!result.ok) {
  console.error("\n❌ 内容校验未通过，共 %d 个问题：\n", result.issues.length);
  for (const issue of result.issues) {
    console.error(`  - ${issue}`);
  }
  process.exit(1);
}

const { countries, regions, programs, glossary } = result.bundle;

console.log("✅ 内容校验通过\n");
console.log(`  国家/地区: ${countries.length}`);
for (const c of countries) {
  const regionCount = regions.filter((r) => r.country === c.id).length;
  const programCount = programs.filter((p) => p.country === c.id).length;
  console.log(
    `    ${c.flag} ${c.name} (${c.id}) — ${regionCount} 个省/州, ${programCount} 个项目`
  );
}
console.log(`  省/州总计: ${regions.length}`);
console.log(`  项目总计:  ${programs.length}`);
console.log(`  术语条目:  ${glossary.length}`);
