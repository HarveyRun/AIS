
(function(){
  const esc8=s=>String(s).replace(/[&<>\"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[m]||m));
  const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
  const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));

  // Add 93: 9 red + 3 blue = C(9,6) × 3 = 252 bets, 504 yuan.
  if(!DATA.prediction.schemes.some(x=>x.code==='93')){
    const core61=DATA.prediction.schemes.find(x=>x.code==='61');
    const reds=[2,6,9,10,13,18,22,25,28];
    const blues=[11,3,15];
    DATA.prediction.schemes.push({
      code:'93',label:'9个红球 + 3个蓝球',note:'扩大红球与蓝球覆盖范围',reds,blues,
      redCount:9,blueCount:3,bets:252,additions:reds.filter(n=>!core61.reds.includes(n)),
      redDetails:reds.map(n=>{const x=redMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.redCount,recent20:x.recent20,recent50:x.recent50,gap:x.gap,status:x.status,role:core61.reds.includes(n)?'核心':'扩展'}}),
      blueDetails:blues.map(n=>{const x=blueMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.count,recent20:x.recent20,gap:x.gap}})
    });
  }

  function schemeCardV8(s,primary=false){
    return '<div class="'+(primary?'scheme-v8 primary-scheme-content':'scheme-card scheme-v8')+'" data-scheme="'+s.code+'">'
      +'<div class="scheme-card-head"><div class="scheme-cost-flow"><b>'+(s.bets*2)+'元</b><span>'+s.bets+'注</span></div></div>'
      +'<div class="ball-row" title="点击号码查看预测原因">'+ballsHTML(s.reds,s.blues)+'</div></div>';
  }
  window.schemeCard=schemeCardV8;
  window.renderSchemes=function(animate=false){
    const schemes=DATA.prediction.schemes;
    const primary=document.getElementById('primaryScheme'),grid=document.getElementById('schemeGrid');
    if(primary)primary.innerHTML=schemeCardV8(schemes[0],true);
    if(grid)grid.innerHTML=schemes.slice(1).map(s=>schemeCardV8(s,false)).join('');
    if(animate){document.querySelectorAll('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}
  };
  window.renderSchemes();

  // Remove obsolete buttons and rename accordion controls.
  ['generateAllBtn','primaryDetailBtn'].forEach(id=>document.getElementById(id)?.remove());
  const addonBtn=document.getElementById('openAddonsBtn');if(addonBtn)addonBtn.textContent='复式预测组';
  const addonSummary=document.querySelector('#addonMaster>summary');if(addonSummary)addonSummary.textContent='复式预测组';

  // Candidate block and recent-draw label.
  const candidateTitle=[...document.querySelectorAll('.side-title')].find(x=>x.textContent.includes('候选')||x.textContent.includes('61组合'));
  if(candidateTitle)candidateTitle.textContent='候选组合';
  const recentTitle=[...document.querySelectorAll('.side-title')].find(x=>x.textContent.includes('最近10期开奖'));
  if(recentTitle)recentTitle.innerHTML='最近10期开奖';

  // Probability page title.
  const probTitle=[...document.querySelectorAll('#forecast .section-head h2')].find(x=>x.textContent.includes('概率'));
  if(probTitle)probTitle.textContent='下期号码概率';

  // Click a red/blue number to open its scheme reason modal.
  function reasonHTML8(s){
    const red=s.reds.map(n=>{const r=redMap.get(n);const role=(s.code==='61'||DATA.prediction.schemes[0].reds.includes(n))?'核心':'扩展';return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+role+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const added=s.additions&&s.additions.length?'<p class="muted">相对核心组合增加红球：'+s.additions.map(pad).join('、')+'。</p>':'';
    return '<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div>'+added+'<div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
  }
  function openReason8(code){
    const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;
    const title=document.getElementById('reasonTitle'),body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal');
    if(title)title.textContent='预测原因';if(body)body.innerHTML=reasonHTML8(s);if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
  }
  ['primaryScheme','schemeGrid'].forEach(id=>{
    const root=document.getElementById(id);if(!root)return;
    root.onclick=e=>{const ball=e.target.closest('.ball');if(!ball)return;const card=ball.closest('[data-scheme]');if(card)openReason8(card.dataset.scheme)};
  });

  // Make the draw red-ball column explicitly width-aware.
  const drawHead=document.querySelector('#drawTable th:nth-child(4)');if(drawHead)drawHead.textContent='红球（6个）';

  // Update glossary to match the current product.
  const featureGrid=document.querySelector('.feature-grid');
  if(featureGrid){
    [...featureGrid.children].forEach(card=>{
      const h=card.querySelector('h3'),p=card.querySelector('p');if(!h||!p)return;
      if(h.textContent==='生成六组号码')card.remove();
      else if(h.textContent==='展开其它5组'){h.textContent='复式预测组';p.textContent='展开查看62、71、72、81、82和93复式号码。';}
      else if(h.textContent==='查看预测原因'){h.textContent='点击号码查看预测原因';p.textContent='点击任意预测组中的红球或蓝球，在弹窗中查看该组每个号码的概率、近期次数、遗漏和排名。';}
      else if(h.textContent.includes('所有号码概率')){h.textContent='下期号码概率';p.textContent='按号码列出01—33红球和01—16蓝球进入下一期的估计概率，每行显示10个。';}
      else if(h.textContent.includes('61另外4组候选')){h.textContent='候选组合';p.textContent='除主预测组外，再展示4组61备选号码，每组2元、1注。';}
      else if(h.textContent==='最近10期开奖'){p.textContent='页面一右侧展示最近10期开奖的期号、日期和号码。';}
    });
    if(![...featureGrid.querySelectorAll('h3')].some(x=>x.textContent==='93方案')){
      featureGrid.insertAdjacentHTML('beforeend','<article class="feature-card"><h3>93方案</h3><p>选择9个红球和3个蓝球，自动拆成252注，购买金额504元。</p></article>');
    }
  }
  document.querySelectorAll('.term-card-v7 h3').forEach(h=>{
    if(h.textContent.includes('61 / 62 / 71 / 72 / 81 / 82')){h.textContent='61 / 62 / 71 / 72 / 81 / 82 / 93';const p=h.nextElementSibling;if(p)p.textContent='前一位表示红球个数，后一位表示蓝球个数；93表示9个红球和3个蓝球。';}
  });
})();
