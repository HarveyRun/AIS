
(function(){
  document.title='智选';
  const brandName=document.querySelector('.brand-copy b');if(brandName)brandName.textContent='智选';

  // Remove only the page-level duplicate title; keep the navigation name and chart group titles.
  const dimensionHead=document.querySelector('#dimensions > .section-head:first-child');
  if(dimensionHead)dimensionHead.remove();

  // Remove the separate top button while keeping the details/summary below fully expandable.
  const addonButton=document.getElementById('openAddonsBtn');
  if(addonButton){const actions=addonButton.closest('.actions');if(actions)actions.remove();else addonButton.remove();}

  // Convert the final multiple group from 9+3 to 8+3.
  const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
  const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));
  const core=DATA.prediction.schemes.find(x=>x.code==='61');
  const ext=DATA.prediction.schemes.find(x=>x.code==='93')||DATA.prediction.schemes.find(x=>x.code==='83');
  if(ext){
    const reds=[2,9,10,13,18,22,25,28],blues=[11,3,15];
    ext.code='83';ext.label='8个红球 + 3个蓝球';ext.note='扩大红球与蓝球覆盖范围';
    ext.reds=reds;ext.blues=blues;ext.redCount=8;ext.blueCount=3;ext.bets=84;
    ext.additions=reds.filter(n=>!core.reds.includes(n));
    ext.redDetails=reds.map(n=>{const x=redMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.redCount,recent20:x.recent20,recent50:x.recent50,gap:x.gap,status:x.status,role:core.reds.includes(n)?'核心':'扩展'}});
    ext.blueDetails=blues.map(n=>{const x=blueMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.count,recent20:x.recent20,gap:x.gap}});
  }
  if(typeof window.renderSchemes==='function')window.renderSchemes(false);

  // Probability cards: number + next-draw probability only.
  window.renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    if(red){red.className='probability-board';red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>').join('')}
    if(blue){blue.className='probability-board';blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>').join('')}
  };
  window.renderProbability();

  // Fill the unused lower space with direct, non-technical result information.
  const main=document.querySelector('#forecast .forecast-main');
  if(main && !document.getElementById('forecastKeyInfo')){
    const s=DATA.prediction.schemes[0],last=DATA.meta.lastDraw;
    const sum=s.reds.reduce((a,b)=>a+b,0),odd=s.reds.filter(n=>n%2).length;
    const repeats=s.reds.filter(n=>last.reds.includes(n));
    const balls=last.reds.map(n=>'<span class="key-info-ball">'+pad(n)+'</span>').join('')+'<span style="color:#7890ad">+</span><span class="key-info-ball blue">'+pad(last.blue)+'</span>';
    const block=document.createElement('section');block.id='forecastKeyInfo';block.className='forecast-key-info';
    block.innerHTML='<h3>主推组概况</h3><div class="key-info-grid">'
      +'<div class="key-info-item"><span class="key-label">上期开奖号码</span><div class="key-info-balls">'+balls+'</div></div>'
      +'<div class="key-info-item"><span class="key-label">红球和值</span><span class="key-value">'+sum+'</span></div>'
      +'<div class="key-info-item"><span class="key-label">红球奇偶</span><span class="key-value">'+odd+' : '+(6-odd)+'</span></div>'
      +'<div class="key-info-item"><span class="key-label">与上期相同红球</span><span class="key-value">'+(repeats.length?repeats.map(pad).join('、'):'无')+'</span></div>'
      +'</div>';
    main.appendChild(block);
  }

  // Keep the glossary/function documentation consistent with 8+3.
  document.querySelectorAll('.feature-card,.term-card-v7').forEach(card=>{
    const h=card.querySelector('h3'),p=card.querySelector('p');if(!h)return;
    if(h.textContent.includes('93方案')){h.textContent=h.textContent.replace('93方案','83方案');if(p)p.textContent='选择8个红球和3个蓝球，自动拆成84注，购买金额168元。';}
    if(h.textContent.includes('/ 93')){h.textContent=h.textContent.replace('/ 93','/ 83');if(p)p.textContent=p.textContent.replace(/93表示9个红球和3个蓝球/g,'83表示8个红球和3个蓝球');}
    if(p){p.textContent=p.textContent.replace(/62、71、72、81、82和93/g,'62、71、72、81、82和83').replace(/9个红球和3个蓝球/g,'8个红球和3个蓝球').replace(/252注/g,'84注').replace(/504元/g,'168元');}
  });

  // Re-apply number click behavior after rebuilding the scheme cards.
  ['primaryScheme','schemeGrid'].forEach(id=>{
    const root=document.getElementById(id);if(!root)return;
    root.onclick=e=>{
      const b=e.target.closest('.ball');if(!b)return;
      const card=b.closest('[data-scheme]');if(!card)return;
      const s=DATA.prediction.schemes.find(x=>x.code===card.dataset.scheme);if(!s)return;
      const red=s.reds.map(n=>{const r=redMap.get(n);return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+(core.reds.includes(n)?'核心':'扩展')+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
      const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
      const heading=document.getElementById('reasonTitle'),body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal');
      if(heading)heading.textContent='预测原因';
      if(body)body.innerHTML='<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div><div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
      if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
    };
  });
})();
