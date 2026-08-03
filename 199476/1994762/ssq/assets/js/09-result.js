
(function(){
  // Restore the exact target issue/date even if an earlier extension script failed.
  const title=document.getElementById('forecastTitle');
  if(title)title.textContent='第 '+DATA.meta.forecastIssue+' 期 · '+DATA.meta.forecastDate;
  const next=document.getElementById('nextIssue');if(next)next.textContent=DATA.meta.forecastIssue;
  const end=document.getElementById('endIssue');if(end)end.textContent=DATA.meta.dataEndIssue;

  // Put prediction numbers and purchase amount on the same row.
  function card(s,primary){
    return '<div class="'+(primary?'scheme-v9 primary-scheme-content':'scheme-card scheme-v9')+'" data-scheme="'+s.code+'">'
      +'<div class="scheme-line"><div class="ball-row" title="点击号码查看预测原因">'+ballsHTML(s.reds,s.blues)+'</div>'
      +'<div class="scheme-cost-inline"><b>'+(s.bets*2)+'元</b><span>'+s.bets+'注</span></div></div></div>';
  }
  window.schemeCard=card;
  window.renderSchemes=function(animate){
    const schemes=DATA.prediction.schemes;
    const primary=document.getElementById('primaryScheme');
    const grid=document.getElementById('schemeGrid');
    if(primary)primary.innerHTML=card(schemes[0],true);
    if(grid)grid.innerHTML=schemes.slice(1).map(s=>card(s,false)).join('');
    if(animate){document.querySelectorAll('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}
  };
  window.renderSchemes(false);

  // Restore all 33 red and 16 blue next-draw probabilities.
  window.renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    if(red){red.className='probability-board';red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('')}
    if(blue){blue.className='probability-board';blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('')}
  };
  window.renderProbability();

  // Rebind number-click reason opening after the cards were rebuilt.
  function openCurrentReason(code){
    const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;
    const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
    const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));
    const red=s.reds.map(n=>{const r=redMap.get(n);return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+(DATA.prediction.schemes[0].reds.includes(n)?'核心':'扩展')+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal'),heading=document.getElementById('reasonTitle');
    if(heading)heading.textContent='预测原因';
    if(body)body.innerHTML='<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div><div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
    if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
  }
  ['primaryScheme','schemeGrid'].forEach(id=>{const root=document.getElementById(id);if(root)root.onclick=e=>{const b=e.target.closest('.ball');if(!b)return;const c=b.closest('[data-scheme]');if(c)openCurrentReason(c.dataset.scheme)}});

  // Force a redraw now that all chart wrapper/tooltip scripts have loaded.
  if(typeof drawVisibleCharts==='function')setTimeout(drawVisibleCharts,80);
})();
