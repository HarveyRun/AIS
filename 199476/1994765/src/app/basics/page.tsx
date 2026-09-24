import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { NoticeBox } from "@/components/notice";
import { getSiteMeta } from "@/lib/content";

export const metadata: Metadata = {
  title: "移民科普：从零讲清楚什么叫移民",
  description: "写给完全没接触过移民的人：什么叫移民、绿卡/永居/入籍是什么关系、五种基本玩法、通用流程，全部用大白话讲。",
};

function QA({ q, children }: { q: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5">
      <p className="font-bold text-slate-900">Q：{q}</p>
      <div className="mt-2 space-y-2 text-sm leading-7 text-slate-600">{children}</div>
    </div>
  );
}

function Block({ n, title, children }: { n: string; title: string; children: React.ReactNode }) {
  return (
    <section className="mt-14">
      <p className="text-xs font-bold tracking-widest text-teal-600">第 {n} 部分</p>
      <h2 className="mt-1 text-2xl font-bold text-slate-900">{title}</h2>
      <div className="mt-5 space-y-4">{children}</div>
    </section>
  );
}

export default function BasicsPage() {
  const site = getSiteMeta();
  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Breadcrumbs items={[{ label: "移民科普" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">移民科普：从零开始</h1>
      <p className="mt-3 leading-8 text-slate-600">
        这一页假设你<b>完全没接触过移民</b>，一个专业词都不认识。
        全篇只讲清楚 4 件事：什么叫移民、移民有哪几种玩法、流程长什么样、去哪里核实信息。
        每个部分都很短，读完大概 10 分钟。
      </p>

      <Block n="01" title="先说清楚：什么叫「移民」">
        <QA q="移民是什么意思？">
          <p>
            一句话：<b>换个国家长期生活，并且拿到那个国家政府发的「许可身份」</b>。
            光人过去不行、旅游签证也不算——移民指的是你的<b>身份</b>变了。
          </p>
        </QA>
        <QA q="身份分几种？">
          <p>拿加拿大举例（美国、澳洲结构一样），身份分三档：</p>
          <p>
            <b>① 临时身份</b>——旅游签、学生签、工签。相当于「限期逗留」：签证到期就得走，工签还绑死雇主，被裁员就可能要离境。
          </p>
          <p>
            <b>② 永久居民（PR，俗称绿卡/枫叶卡）</b>——想住多久住多久，正常工作、上学、上保险，除了投票和当公务员，待遇和本国人基本一样。<b>但护照还是中国护照</b>。
          </p>
          <p>
            <b>③ 公民（入籍）</b>——永居住满一定年限（加拿大 3 年、澳洲 4 年）后可以申请，换发该国护照。
          </p>
          <p>
            所以：大家平常说的「移民成功」，<b>绝大多数时候指的是拿到②永久居民</b>，不是换国籍。
          </p>
        </QA>
        <QA q="拿到永居就一劳永逸了吗？">
          <p>
            不是。永居有「居住义务」，俗称<b>移民监</b>：比如加拿大要求 5 年内实际住满 2 年，
            住不够，永居身份可能被取消。很多人拿到身份后继续在国内生活，几年后想回加拿大才发现身份没了。
          </p>
        </QA>
      </Block>

      <Block n="02" title="移民有哪几种基本玩法">
        <p className="text-sm leading-7 text-slate-600">
          全世界的移民项目五花八门，但底层逻辑只有 5 种。你可以对号入座，看自己能走哪一种：
        </p>
        <QA q="① 留学转移民——先读书，再留下">
          <p>
            先申请学校拿学签去读书，毕业后拿「毕业工签」找工作，再走毕业生通道拿身份。
            <b>花的不是钱，是学费 + 2-4 年时间</b>，但对年轻、条件一般的人是最稳的入场券。加拿大、澳洲这条路线最成熟。
          </p>
        </QA>
        <QA q="② 雇主担保——找到当地雇主给你发工资">
          <p>
            当地公司雇你，由公司出面担保你拿身份。政府怕你抢本地人饭碗，常要求雇主先证明「招不到本地人」。
            <b>核心就一件事：拿到那份 offer。</b>门槛比技术移民低，英语要求也低。例子：加拿大各省雇主担保、美国 EB-2/EB-3。
          </p>
        </QA>
        <QA q="③ 技术移民——凭自身条件「打分上岸」">
          <p>
            政府把年龄、学历、英语、工作经验折算成分数，所有人排队，从高往低邀请。
            <b>不需要雇主、不需要花钱</b>，但要求你本身条件好：年轻、学历高、英语强。
            例子：加拿大 Express Entry、澳洲 189。
          </p>
        </QA>
        <QA q="④ 家庭团聚——家里有人已经是当地人">
          <p>
            配偶、父母、子女是该国公民或永居，可以担保你。<b>不看条件，只看关系真实性</b>。假结婚是刑事犯罪，别碰。
          </p>
        </QA>
        <QA q="⑤ 投资移民——用钱和生意说话">
          <p>
            出资在当地投资或开公司、雇当地人，换取身份。<b>不看学历年龄语言，看钱和钱的来路</b>。
            例子：美国 EB-5（80 万美元起）、澳洲 188/888、加拿大 SUV 创业移民。
          </p>
        </QA>
      </Block>

      <Block n="03" title="不管走哪条路，流程都长这样">
        <ol className="space-y-3">
          {[
            { t: "给自己做个体检", d: "年龄、学历、英语水平、工作年限、职业、能拿出多少钱。这决定了你适合上面哪一种玩法。" },
            { t: "选国家和省/州/区", d: "同一个国家里，不同省/州/区的政策可能完全不同。在本站按国家 → 省/州/区逐个看。" },
            { t: "锁定具体项目", d: "进项目页逐条核对官方要求，确认自己够不够格。" },
            { t: "补硬件", d: "考英语（或法语）、做学历认证、职业评估——这些通常要几个月，是所有人的必经工序。" },
            { t: "递交、审核", d: "按官方清单交材料，之后是体检和背景调查，中间可能被要求补料。" },
            { t: "登陆、守住身份", d: "获批后限期入境激活；之后记得满足居住义务，别把身份睡没了。" },
          ].map((s, i) => (
            <li key={s.t} className="flex gap-3 rounded-2xl border border-slate-200 bg-white p-4">
              <span aria-hidden className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-teal-600 text-sm font-bold text-white">
                {i + 1}
              </span>
              <div>
                <p className="font-semibold text-slate-900">{s.t}</p>
                <p className="mt-0.5 text-sm leading-relaxed text-slate-600">{s.d}</p>
              </div>
            </li>
          ))}
        </ol>
      </Block>

      <Block n="04" title="几个新手必问的问题">
        <QA q="不会英语能移民吗？">
          <p>
            能，选择会变少：投资类基本不看英语；雇主担保里部分技工、护理岗位英语门槛很低；
            完全不看的只有家庭团聚。但英语越好，可选的路越多，这是实话。
          </p>
        </QA>
        <QA q="预算大概要多少？">
          <p>
            技术移民最省钱（几万人民币办理成本）；雇主担保看有没有真实 offer，有 offer 规费不高；
            留学转移民要算学费生活费；投资移民从几十万到上千万人民币不等。
            每个项目页都有<b>逐项费用明细</b>：官方规费、中介服务、登陆后的生活开销，一项一项列，不给拍脑袋的总价。
          </p>
        </QA>
        <QA q="被拒签过还能再申请吗？">
          <p>
            能，但后续申请通常要如实申报拒签史，隐瞒本身就是新的拒签理由。
            重要的是搞清上次为什么被拒，把问题解决了再递。
          </p>
        </QA>
        <QA q="更多问题去哪问？">
          <p>
            去看<b>常见问题页</b>（按国家整理），或者先逛
            <Link href="/glossary/" className="mx-1 font-medium text-teal-700 underline underline-offset-2">术语表</Link>
            把基本词汇混个脸熟。
          </p>
        </QA>
      </Block>

      <Block n="05" title="最后：认准官方">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 text-sm leading-7 text-slate-600">
          <p>
            加拿大看
            <a href="https://www.canada.ca/en/immigration-refugees-citizenship.html" target="_blank" rel="noopener noreferrer" className="mx-1 font-medium text-teal-700 underline underline-offset-2">IRCC 官网 ↗</a>
            ，澳洲看
            <a href="https://immi.homeaffairs.gov.au/" target="_blank" rel="noopener noreferrer" className="mx-1 font-medium text-teal-700 underline underline-offset-2">内政部官网 ↗</a>
            ，美国看
            <a href="https://www.uscis.gov/" target="_blank" rel="noopener noreferrer" className="mx-1 font-medium text-teal-700 underline underline-offset-2">USCIS 官网 ↗</a>
            。本站每个项目页都附官方链接和更新时间；任何第三方说法（包括本站），动手申请前都回官方页面核对一遍。
          </p>
        </div>
      </Block>
    </div>
  );
}
