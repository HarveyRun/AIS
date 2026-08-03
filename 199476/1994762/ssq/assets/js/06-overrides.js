
(function(){
  // Restore the V2 logo exactly.
  const brand=document.querySelector('.brand');
  if(brand){
    const old=brand.querySelector('.six-logo');
    if(old){const logo=document.createElement('div');logo.className='logo';logo.textContent='6+1';old.replaceWith(logo)}
  }

  // Restore the former right-side quality area as two business-focused blocks.
  const mainPanel=document.querySelector('#forecast .forecast-main');
  if(mainPanel && !document.querySelector('.forecast-hero')){
    const hero=document.createElement('div');hero.className='forecast-hero';
    mainPanel.parentNode.insertBefore(hero,mainPanel);hero.appendChild(mainPanel);
    const aside=document.createElement('aside');aside.className='result-side';aside.innerHTML='<div class="panel side-block"><div class="side-title">最近10期开奖</div><div class="recent-window"><div class="recent-track" id="recentTrack"></div></div></div><div class="panel side-block"><div class="side-title">候选组合</div><div class="alt-candidates" id="altCandidates"></div></div>';
    hero.appendChild(aside);
  }
  function smallBalls(reds,blue){return reds.map(n=>'<span class="tiny-result-ball">'+pad(n)+'</span>').join('')+'<span class="tiny-result-ball blue">'+pad(blue)+'</span>'}
  const recent=DATA.stats.draws.slice(-10).reverse();
  const recentHTML=recent.map(d=>'<div class="recent-row"><span class="issue">'+d.issue+'<br>'+d.date.slice(5)+'</span><span class="recent-balls">'+smallBalls(d.reds,d.blue)+'</span><span></span></div>').join('');
  const rt=document.getElementById('recentTrack');if(rt)rt.innerHTML=recentHTML+recentHTML;
  const ac=document.getElementById('altCandidates');if(ac)ac.innerHTML=DATA.prediction.topCandidates.slice(1,4).map((c,i)=>'<div class="alt-candidate"><div class="alt-candidate-head"><b>候选'+(i+1)+'</b><span>2元</span></div><div class="alt-candidate-balls">'+c.reds.map(n=>'<span class="mini">'+pad(n)+'</span>').join('')+'<span class="mini blue">'+pad(c.blue)+'</span></div></div>').join('');

  // Modal shell.
  if(!document.getElementById('reasonModal')){
    document.body.insertAdjacentHTML('beforeend','<div class="reason-modal" id="reasonModal"><div class="reason-dialog" role="dialog" aria-modal="true" aria-labelledby="reasonTitle"><div class="reason-head"><h2 id="reasonTitle">预测原因</h2><button class="reason-close" id="reasonClose" aria-label="关闭">×</button></div><div id="reasonBody"></div></div></div>');
  }
  function reasonHTML(s){
    const red=s.redDetails.map(r=>'<div class="reason-row-v6"><span class="num-dot">'+pad(r.number)+'</span><div class="main"><b>'+esc(r.role)+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.prob)+'</div></div>').join('');
    const blue=s.blueDetails.map(r=>'<div class="reason-row-v6"><span class="num-dot blue">'+pad(r.number)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.prob)+'</div></div>').join('');
    const added=s.additions&&s.additions.length?'<p class="muted">本组在61核心红球上增加：'+s.additions.map(pad).join('、')+'。</p>':'';
    return '<div class="reason-summary"><span class="scheme-code">'+s.code+'</span><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">购买金额：'+(s.bets*2)+'元</span></div>'+added+'<div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
  }
  function openReason(code){const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;document.getElementById('reasonTitle').textContent=s.code+'组合预测原因';document.getElementById('reasonBody').innerHTML=reasonHTML(s);document.getElementById('reasonModal').classList.add('open');document.body.style.overflow='hidden'}
  function closeReason(){document.getElementById('reasonModal').classList.remove('open');document.body.style.overflow=''}
  document.getElementById('reasonClose').onclick=closeReason;
  document.getElementById('reasonModal').onclick=e=>{if(e.target.id==='reasonModal')closeReason()};
  document.addEventListener('keydown',e=>{if(e.key==='Escape')closeReason()});

  // Replace scheme rendering: show price, remove technical KPI blocks, use modal details.
  schemeCard=function(s,primary=false){
    const detailButton=primary?'':'<div class="actions" style="margin-top:12px"><button class="btn btn-blue scheme-reason-btn" data-code="'+s.code+'">查看'+s.code+'预测原因</button></div>';
    return '<div class="'+(primary?'':'scheme-card')+'"><div class="scheme-card-head"><div><span class="scheme-code">'+s.code+'</span><span class="scheme-name">'+esc(s.label)+'</span></div></div><div class="ball-row">'+ballsHTML(s.reds,s.blues)+'</div><div class="scheme-price"><span>购买金额</span><b>'+s.bets*2+'元</b><small>'+s.bets+'注</small></div>'+detailButton+'</div>';
  };
  bindSchemeButtons=function(){$$('.scheme-reason-btn').forEach(b=>b.onclick=()=>openReason(b.dataset.code))};
  renderSchemes=function(animate=false){const schemes=DATA.prediction.schemes;$('#primaryScheme').innerHTML=schemeCard(schemes[0],true);$('#schemeGrid').innerHTML=schemes.slice(1).map(s=>schemeCard(s)).join('');bindSchemeButtons();if(animate){$$('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}};
  renderSchemes();
  {const el=document.getElementById('generateAllBtn');if(el)el.onclick=()=>renderSchemes(true);}
  {const el=document.getElementById('primaryDetailBtn');if(el)el.onclick=()=>openReason('61');}

  // Rebuild probability area with number + next-draw probability only.
  renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    red.className='probability-board';blue.className='probability-board';
    red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('');
    blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('');
  };
  renderProbability();
  const redTitle=document.querySelector('#redHeat')?.closest('.panel')?.querySelector('h3');if(redTitle)redTitle.textContent='红球';
  const blueTitle=document.querySelector('#blueHeat')?.closest('.panel')?.querySelector('h3');if(blueTitle)blueTitle.textContent='蓝球';

  // Professional page becomes a direct glossary.
  const navProf=document.querySelector('[data-page="professional"]');if(navProf)navProf.textContent='术语说明';
  const terms=[
    ['进入下一期概率','系统根据历史数据计算的单个号码在下一期出现的估计比例。'],['复式','一次选择超过6个红球或超过1个蓝球，系统自动拆成多注标准号码。'],['注数','一组标准的6个红球加1个蓝球算1注。'],['61 / 62 / 71 / 72 / 81 / 82','前一位表示红球个数，后一位表示蓝球个数。'],['近20期','只统计最近20次开奖。'],['历史次数','该号码在全部历史开奖中出现的总次数。'],['遗漏期数','从该号码上次出现到现在，连续没有出现的期数。'],['平均遗漏','历史上两次出现之间，平均间隔多少期。'],['最大遗漏','历史记录中连续没有出现的最长间隔。'],['和值','6个红球相加后的总数。'],['跨度','最大红球减去最小红球。'],['奇偶比','6个红球中奇数和偶数的数量比例。'],['大小比','1—16为小号，17—33为大号，两类号码的数量比例。'],['三区比','01—11、12—22、23—33三个区间各有几个红球。'],['连号','同一期中相邻的号码，例如09和10。'],['重号','与上一期重复出现的红球。'],['尾数','号码个位数，例如09和19的尾数都是9。'],['号码分散度（AC值）','用号码之间不同差值的多少，表示一组号码分散还是集中。'],['共现次数','两个或三个号码在同一期一起出现过多少次。'],['共同出现倍数','一组号码实际一起出现的频率，相对各自独立出现时高出多少。'],['PMI','衡量两个号码一起出现是否比随机组合更频繁。'],['Z值','某个号码的历史次数距离平均水平有多远。'],['卡方检验','检查所有号码的历史次数是否明显偏离平均分布。'],['p值','用来衡量观察到的差异由随机波动造成的可能程度。'],['相关系数','衡量两类数据一起变化的方向和强弱。'],['时间切分回测','只用过去数据预测后面的期次，避免提前看到答案。'],['逻辑回归','根据多个历史因素计算号码出现概率的算法。'],['梯度提升','通过多轮修正错误来寻找复杂关系的算法。'],['极端随机树','用大量随机决策树综合计算结果的算法。'],['对数损失','衡量概率预测是否准确；越小通常越好。']
  ];
  const prof=document.getElementById('professional');if(prof)prof.innerHTML='<div class="section-head"><div><h2>术语说明</h2></div></div><div class="term-grid">'+terms.map(t=>'<article class="term-card"><h3>'+esc(t[0])+'</h3><p>'+esc(t[1])+'</p></article>').join('')+'</div>';

  // Generic tooltip for every Canvas chart renderer.
  if(!document.getElementById('chartTooltip'))document.body.insertAdjacentHTML('beforeend','<div class="chart-tooltip" id="chartTooltip"></div>');
  const tip=document.getElementById('chartTooltip');
  function vfmt(v,label){if(v==null||Number.isNaN(Number(v)))return String(v);const n=Number(v);if((label||'').includes('元'))return n.toLocaleString('zh-CN',{maximumFractionDigits:0})+'元';if((label||'').includes('%')||(label||'').includes('比例')||(label||'').includes('命中率'))return n.toFixed(2)+(label.includes('%')?'%':'');if(Number.isInteger(n))return n.toLocaleString('zh-CN');return n.toFixed(Math.abs(n)<1?4:2)}
  function attachTooltip(id,labels,series,opt={},orientation='x',mode='bar'){
    const c=document.getElementById(id);if(!c)return;c.style.cursor='crosshair';
    c.onmousemove=function(e){const r=c.getBoundingClientRect(),mx=e.clientX-r.left,my=e.clientY-r.top;let idx=-1;
      if(orientation==='y'){const top=18,bottom=48,ph=r.height-top-bottom;if(my<top||my>top+ph)return tip.classList.remove('show');idx=Math.floor((my-top)/ph*labels.length)}
      else{const left=opt.left||68,right=18,top=18,bottom=opt.bottom||62,pw=r.width-left-right;if(mx<left||mx>left+pw||my<top||my>r.height-bottom)return tip.classList.remove('show');const ratio=(mx-left)/pw;idx=mode==='line'?Math.round(ratio*(labels.length-1)):Math.floor(ratio*labels.length)}
      idx=Math.max(0,Math.min(labels.length-1,idx));let html='<b>'+esc(String(labels[idx]))+'</b>';series.forEach(s=>{html+='<span><i>'+esc(s.name||opt.yLabel||'数值')+'</i><em>'+vfmt(s.values[idx],s.name||opt.yLabel||'')+'</em></span>'});tip.innerHTML=html;tip.classList.add('show');const tw=tip.offsetWidth,th=tip.offsetHeight;tip.style.left=Math.min(window.innerWidth-tw-10,e.clientX+14)+'px';tip.style.top=Math.min(window.innerHeight-th-10,e.clientY+14)+'px'};
    c.onmouseleave=()=>tip.classList.remove('show');
  }
  const oldBar=barChart,oldLine=lineChart,oldGroup=groupedBarChart,oldMulti=multiLineChart,oldH=hBarChart,oldSigned=signedBarChart;
  barChart=function(id,labels,vals,opt={}){oldBar(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','bar')};
  lineChart=function(id,labels,vals,opt={}){oldLine(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','line')};
  groupedBarChart=function(id,labels,series,opt={}){oldGroup(id,labels,series,opt);attachTooltip(id,labels,series,opt,'x','bar')};
  multiLineChart=function(id,labels,series,opt={}){oldMulti(id,labels,series,opt);attachTooltip(id,labels,series,opt,'x','line')};
  hBarChart=function(id,labels,vals,opt={}){oldH(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.xLabel||'数值',values:vals}],opt,'y','bar')};
  signedBarChart=function(id,labels,vals,opt={}){oldSigned(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','bar')};
  drawVisibleCharts();
})();
