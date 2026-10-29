'use strict';
const $ = id => document.getElementById(id);
const demo = new URLSearchParams(location.search).get('demo') === '1';
let config = {slideSeconds:20, refreshSeconds:900, maxAgeHours:72};
let articles = [], active = 0, started = performance.now(), sources = [], generatedAt = null, lastPayload = null;
let refreshFailed = false;
const progressKey = 'fil-info-progress-v1:' + location.pathname;
let savedProgress = readProgress();
let rotationHidden = document.hidden;
let hasPresentedArticle = false;
function readProgress(){
  if(demo)return null;
  try{const value=JSON.parse(localStorage.getItem(progressKey));return value?.version===1&&Array.isArray(value.nextIds)&&Number.isInteger(value.nextIndex)&&value.nextIndex>=0?value:null;}catch{return null;}
}
function rememberProgress(){
  if(demo||!articles.length||rotationHidden)return;
  hasPresentedArticle=true;
  const nextIds=Array.from({length:articles.length},(_,i)=>articles[(active+i+1)%articles.length].id);
  savedProgress={version:1,nextIds,nextIndex:(active+1)%articles.length};
  try{localStorage.setItem(progressKey,JSON.stringify(savedProgress));}catch{}
}
function resumeIndex(list,progress){
  if(!list.length||!progress)return 0;
  for(const id of progress.nextIds){const index=list.findIndex(a=>a.id===id);if(index>=0)return index;}
  return progress.nextIndex%list.length;
}
function nextArticle(){
  if(!articles.length)return;
  active=(active+1)%articles.length;started=performance.now();render();
}
const dateFormat = new Intl.DateTimeFormat('fr-FR',{timeZone:'Europe/Paris',day:'numeric',month:'long',weekday:'long'});
const timeFormat = new Intl.DateTimeFormat('fr-FR',{timeZone:'Europe/Paris',hour:'2-digit',minute:'2-digit'});
const stampFormat = new Intl.DateTimeFormat('fr-FR',{timeZone:'Europe/Paris',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
function resize(){const scale=Math.min(innerWidth/1920,innerHeight/1080);$('screen').style.transform=`scale(${scale})`;$('screen').style.left=`${(innerWidth-1920*scale)/2}px`;$('screen').style.top=`${(innerHeight-1080*scale)/2}px`;}
function clock(){const now=new Date();$('date').textContent=dateFormat.format(now);$('time').textContent=timeFormat.format(now);}
function safeURL(value){try{const u=new URL(value);return ['https:','http:'].includes(u.protocol)&&!u.username?u.href:'';}catch{return '';}}
function formatStamp(value){return value&&!Number.isNaN(Date.parse(value))?stampFormat.format(new Date(value)):'';}
function imageInto(img,url){img.hidden=true;img.onload=()=>img.hidden=false;img.onerror=()=>img.hidden=true;const safe=safeURL(url);if(safe){img.src=safe;}else{img.removeAttribute('src');}}
function sourceStamp(a){const date=formatStamp(a.published);return `${a.source}${date?' · '+date:''}`;}
function render(){
  if(!articles.length){
    $('hero-image').hidden=true;$('category').textContent='LE FIL INFO';$('source').textContent='ACTUALITÉS INDISPONIBLES';
    $('headline').textContent='Les informations reviennent bientôt.';
    $('summary').textContent='Aucun article récent disponible. Nouvelle tentative automatique dans quelques minutes.';
    $('article-link').hidden=true;$('up-next').replaceChildren();$('counter').textContent='— / —';$('ticker').textContent='En attente de nouvelles actualités.';return;
  }
  const a=articles[active];$('category').textContent=a.category;$('source').textContent=sourceStamp(a);$('headline').textContent=a.title;$('summary').textContent=a.summary||'';
  rememberProgress();
  const link=safeURL(a.url);$('article-link').hidden=!link;$('article-link').href=link||'#';
  imageInto($('hero-image'),a.image);$('image-note').textContent=a.image?'IMAGE DU FLUX ÉDITEUR':'SANS PHOTO DANS LE FLUX';
  $('counter').textContent=`${String(active+1).padStart(2,'0')} / ${String(articles.length).padStart(2,'0')}`;
  $('hero').classList.remove('changing');void $('hero').offsetWidth;$('hero').classList.add('changing');
  $('up-next').replaceChildren();
  for(let i=1;i<=Math.min(3,articles.length-1);i++){
    const item=articles[(active+i)%articles.length], row=document.createElement('article');row.className='next';
    const thumb=document.createElement('div');thumb.className='thumb';const letter=document.createElement('span');letter.textContent=item.source.slice(0,1);thumb.append(letter);
    if(safeURL(item.image)){const image=document.createElement('img');image.alt='';imageInto(image,item.image);thumb.append(image);}
    const copy=document.createElement('div'),category=document.createElement('span'),title=document.createElement('h3'),source=document.createElement('div');
    category.className='next-category';category.textContent=item.category;title.textContent=item.title;source.className='next-source';source.textContent=item.source;
    copy.append(category,title,source);row.append(thumb,copy);$('up-next').append(row);
  }
}
function ticker(){const node=$('ticker');node.replaceChildren();if(!articles.length)return;
  const list=articles.slice(0,12);for(let repeat=0;repeat<2;repeat++)for(const a of list){const b=document.createElement('b');b.textContent=a.title;const separator=document.createElement('i');separator.textContent='•';node.append(b,separator);}
  node.style.animationDuration=`${Math.max(60,list.reduce((n,a)=>n+a.title.length,0)/5)}s`;
}
function freshness(){
  const age=generatedAt?Date.now()-Date.parse(generatedAt):Infinity;
  const failures=sources.filter(s=>s.status!=='ok').length;
  const warning=refreshFailed||age>2*3600000||failures>0;
  $('status').classList.toggle('warning',warning);
  $('status').textContent=demo?'MODE DÉMONSTRATION':refreshFailed?'CONNEXION INTERROMPUE · DERNIERS TITRES CONSERVÉS':age>2*3600000?'MISE À JOUR EN RETARD':failures?`${failures} SOURCE${failures>1?'S':''} TEMPORAIREMENT INDISPONIBLE${failures>1?'S':''}`:'LES DERNIERS TITRES DES RÉDACTIONS';
  $('freshness').textContent=demo?'APERÇU DU DESIGN':generatedAt?'COLLECTE : '+formatStamp(generatedAt):'EN ATTENTE DE LA PREMIÈRE COLLECTE';
}
function applyData(data){
  if(!Array.isArray(data.articles)||!Array.isArray(data.sources))throw Error('Données invalides');
  sources=data.sources;generatedAt=data.generatedAt;
  const cutoff=Date.now()-config.maxAgeHours*3600000;
  const oldId=articles[active]?.id;
  articles=data.articles.filter(a=>a&&typeof a.title==='string'&&typeof a.source==='string'&&(demo||Date.parse(a.published||sources.find(s=>s.id===a.feedId)?.lastSuccess)>cutoff));
  const preserved=articles.findIndex(a=>a.id===oldId);
  active=preserved>=0?preserved:resumeIndex(articles,savedProgress);
  if(preserved<0)started=performance.now();
  render();ticker();freshness();
}
async function load(){
  if(demo)return;
  try{const response=await fetch('news.json?t='+Date.now(),{cache:'no-store',signal:AbortSignal.timeout(15000)});if(!response.ok)throw Error('HTTP');const data=await response.json();refreshFailed=false;applyData(data);lastPayload=data;try{localStorage.setItem('fil-info-cache',JSON.stringify(data));}catch{}}
  catch{refreshFailed=true;if(!lastPayload){try{lastPayload=JSON.parse(localStorage.getItem('fil-info-cache'));if(lastPayload)applyData(lastPayload);}catch{}}if(!articles.length)render();freshness();}
}
function loadDemo(){
  $('demo-label').hidden=false;
  const entries=[['Le Dauphiné Libéré','ISÈRE','Les initiatives locales à la une','Projets, événements et vie locale : les titres de votre département apparaîtront dans cet espace.'],['Le Monde','À LA UNE','L’essentiel de l’actualité française et internationale','Le titre, la date et le résumé transmis par le flux RSS remplaceront cet exemple.'],['Secours Mag','POMPIERS & SECOURS','Un regard sur les métiers du secours','Une rubrique dédiée à la prévention, au secourisme et aux acteurs de la sécurité civile.'],['Franceinfo','FRANCE & MONDE','Les derniers titres défilent automatiquement','Une nouvelle actualité toutes les vingt secondes, sans intervention sur l’écran.']];
  applyData({generatedAt:new Date().toISOString(),sources:[],articles:entries.map(([source,category,title,summary],i)=>({id:'demo'+i,source,category,title,summary,published:null,image:'',url:''}))});
}
async function init(){resize();clock();setInterval(clock,1000);addEventListener('resize',resize);
  try{const r=await fetch('config.json');if(r.ok)config={...config,...await r.json()};}catch{}
  config.slideSeconds=Math.max(10,Number(config.slideSeconds)||20);config.refreshSeconds=Math.max(60,Number(config.refreshSeconds)||900);
  if(config.title)$('brand').textContent=config.title;if(config.subtitle)$('subtitle').textContent=config.subtitle;
  document.title=config.title||'Le Fil Info';if(demo)loadDemo();else await load();
  setInterval(load,config.refreshSeconds*1000);
  document.addEventListener('visibilitychange',()=>{
    if(document.hidden){rotationHidden=true;return;}
    if(rotationHidden){rotationHidden=false;if(hasPresentedArticle)nextArticle();else{started=performance.now();render();}}
  });
  setInterval(()=>{if(!articles.length||rotationHidden)return;const elapsed=performance.now()-started;$('progress').style.width=Math.min(100,elapsed/(config.slideSeconds*10))+'%';if(elapsed>=config.slideSeconds*1000)nextArticle();},100);
  setInterval(()=>{if(lastPayload)applyData(lastPayload);else freshness();},60000);
}
init();
