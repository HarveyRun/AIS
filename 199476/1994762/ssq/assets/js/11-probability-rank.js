
(function(){
  function renderSortedProbability(){
    const render=(id,items,isBlue)=>{
      const el=document.getElementById(id);if(!el)return;
      el.className='probability-board';
      const sorted=[...items].sort((a,b)=>(b.modelProb-a.modelProb)||(a.number-b.number));
      el.innerHTML=sorted.map((x,i)=>{
        const top=i<3?' prob-top-'+(i+1):'';
        const mark=i<3?'<span class="prob-rank-mark">'+(i+1)+'</span>':'';
        return '<div class="next-prob'+(isBlue?' blue':'')+top+'">'+mark+'<span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>';
      }).join('');
    };
    render('redHeat',DATA.probabilities.red,false);
    render('blueHeat',DATA.probabilities.blue,true);
  }
  window.renderProbability=renderSortedProbability;
  renderSortedProbability();
})();
