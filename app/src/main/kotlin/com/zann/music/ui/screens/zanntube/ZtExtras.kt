package com.zann.music.ui.screens.zanntube

const val ZT_EXTRA_JS = ZT_ADS_JS + ZT_BG_JS + """(function(){
if(window.__ztx2)return;window.__ztx2=1;
var D={ab:1,bg:1,pp:1,hs:1,rs:1,sb:1,dl:1,qs:'auto',qv:'auto',sp:1,ms:'default'};
function lc(){try{var o=JSON.parse(localStorage.getItem('zt_cfg')||'{}');for(var k in D)if(o[k]===undefined)o[k]=D[k];if(!o._m2){o.hs=1;o._m2=1;}return o;}catch(e){return JSON.parse(JSON.stringify(D));}}
var cfg=lc();
try{ZtBridge.miniStyle(cfg.ms);}catch(e){}
function set(k,v){cfg[k]=v;try{localStorage.setItem('zt_cfg',JSON.stringify(cfg));}catch(e){}apply();}
function el(t,css,tx){var e=document.createElement(t);if(css)e.style.cssText=css;if(tx!==undefined)e.textContent=tx;return e;}
function vid(){var m=location.search.match(/[?&]v=([A-Za-z0-9_-]{11})/);return(location.pathname.indexOf('/watch')===0&&m)?m[1]:null;}
function isSh(){return location.pathname.indexOf('/shorts')===0;}
function vel(){return document.querySelector('video');}
var QS=[['Auto','auto'],['144p','tiny'],['240p','small'],['360p','medium'],['480p','large'],['720p','hd720'],['1080p','hd1080']];
var SP=[['0.5x','0.5'],['0.75x','0.75'],['1x','1'],['1.25x','1.25'],['1.5x','1.5'],['2x','2']];
function lbl(q){for(var i=0;i<QS.length;i++)if(QS[i][1]===q)return QS[i][0];return 'Auto';}

var sy=0,dy=0,act=false,ind=null,busy=false;
function se(){return document.scrollingElement||document.documentElement;}
function okp(){var p=location.pathname;return p.indexOf('/watch')!==0&&p.indexOf('/shorts')!==0&&p.indexOf('/live_chat')!==0;}
function mk(){
 if(ind&&ind.isConnected)return ind;
 ind=el('div','position:fixed;top:0;left:50%;width:42px;height:42px;margin-left:-21px;border-radius:50%;background:#2b2930;color:#d0bcff;z-index:2147483647;display:flex;align-items:center;justify-content:center;font:bold 22px sans-serif;transform:translateY(-70px);box-shadow:0 2px 10px rgba(0,0,0,.6);pointer-events:none;','\u21BB');
 document.body.appendChild(ind);return ind;}
var O={passive:true,capture:true};
document.addEventListener('touchstart',function(e){
 if(busy||!okp()||e.touches.length!==1||se().scrollTop>0){act=false;return;}
 sy=e.touches[0].clientY;dy=0;act=true;},O);
document.addEventListener('touchmove',function(e){
 if(!act)return;
 dy=e.touches[0].clientY-sy;
 var i=mk();
 if(se().scrollTop>0||dy<=0){i.style.transform='translateY(-70px)';return;}
 i.style.transition='none';
 i.style.transform='translateY('+(Math.min(dy*0.45,90)-70)+'px) rotate('+(dy*2)+'deg)';},O);
function fin(){
 if(!act)return;act=false;
 var i=mk();
 if(dy>170&&se().scrollTop<=0){busy=true;i.style.transition='transform .2s';i.style.transform='translateY(30px)';
  setTimeout(function(){location.reload();},150);}
 else{i.style.transition='transform .2s';i.style.transform='translateY(-70px)';}}
document.addEventListener('touchend',fin,O);
document.addEventListener('touchcancel',fin,O);

var mx=0,my=0,ma=false;
function minimize(){
 var id=vid(),v=vel();if(!id||!v)return;
 var o=document.querySelector('ytm-slim-owner-renderer');
 var ch=((o&&o.innerText)||'').split('\n')[0];
 var ti=document.title.replace(/ - YouTube$/,'');
 try{ZtBridge.minimize(id,Math.floor(v.currentTime),ti,ch);}catch(e){return;}
 try{v.pause();}catch(e){}
 window.__ztHome();}
document.addEventListener('touchstart',function(e){
 ma=false;
 if(!vid()||document.fullscreenElement||e.touches.length!==1)return;
 var v=vel();if(!v)return;
 var r=v.getBoundingClientRect(),t=e.touches[0];
 if(t.clientY<r.top||t.clientY>r.top+r.height*0.8||t.clientX<r.left||t.clientX>r.right)return;
 mx=t.clientX;my=t.clientY;ma=true;},O);
document.addEventListener('touchend',function(e){
 if(!ma)return;ma=false;
 var t=e.changedTouches[0],d=t.clientY-my,x=Math.abs(t.clientX-mx);
 if(d>110&&x<d*0.6)minimize();},O);

function hs(){
 var s=document.getElementById('zt-hs');
 if(cfg.hs){if(!s){s=el('style');s.id='zt-hs';s.textContent='ytm-reel-shelf-renderer,ytm-shorts-lockup-view-model,ytm-shorts-lockup-view-model-v2,ytm-rich-section-renderer:has(ytm-reel-shelf-renderer),ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model),ytm-rich-section-renderer:has(a[href^="/shorts/"]),ytm-item-section-renderer:has(ytm-reel-shelf-renderer),ytm-rich-item-renderer:has(a[href^="/shorts/"]),ytm-video-with-context-renderer:has(a[href^="/shorts/"]),ytm-compact-video-renderer:has(a[href^="/shorts/"]),grid-shelf-view-model:has(a[href^="/shorts/"]){display:none !important;}';(document.head||document.documentElement).appendChild(s);}}
 else if(s)s.remove();}
function rs(){
 var s=document.getElementById('zt-rs');
 if(cfg.rs){if(!s){s=el('style');s.id='zt-rs';s.textContent='html,body,ytm-app{--yt-spec-base-background:#141218 !important;background:#141218 !important;}ytm-rich-item-renderer,ytm-video-with-context-renderer,ytm-compact-video-renderer{background:#1d1b20 !important;border-radius:22px !important;overflow:hidden !important;margin:8px 12px !important;padding-bottom:6px !important;}ytm-rich-item-renderer img,ytm-video-with-context-renderer img,ytm-compact-video-renderer img{border-radius:18px !important;}chip-shape button,chip-shape,ytm-chip-cloud-chip-renderer{border-radius:16px !important;}';(document.head||document.documentElement).appendChild(s);}}
 else if(s)s.remove();}
function ap(){
 var q=isSh()?cfg.qs:(vid()?cfg.qv:'auto');if(q==='auto')return;
 var p=document.querySelector('.html5-video-player')||document.getElementById('movie_player');
 if(!p)return;
 try{
  if(p.getPlaybackQuality&&p.getPlaybackQuality()===q)return;
  if(p.setPlaybackQualityRange)p.setPlaybackQualityRange(q,q);
  if(p.setPlaybackQuality)p.setPlaybackQuality(q);
 }catch(e){}}
var spId='';
function spd(){
 var id=vid();if(!id){spId='';return;}
 var v=vel();if(!v||id===spId||v.readyState<1)return;
 spId=id;if(cfg.sp!=1)v.playbackRate=cfg.sp;}
var sbId='',sbSeg=[];
window.__ztSb=function(id,a){if(id===sbId)sbSeg=a||[];};
function sbLoad(){
 var id=vid();
 if(!cfg.sb||!id){sbId='';sbSeg=[];return;}
 if(id!==sbId){sbId=id;sbSeg=[];try{ZtBridge.sponsor(id);}catch(e){}}}
setInterval(function(){
 if(!cfg.sb||!sbSeg.length||!vid())return;
 var v=vel();if(!v)return;
 var t=v.currentTime;
 for(var i=0;i<sbSeg.length;i++){var s=sbSeg[i];if(t>=s[0]&&t<s[1]-0.4){v.currentTime=s[1];break;}}
},300);

var dl=null,dm=null;
function go(au){dm.style.display='none';var id=vid();if(!id)return;try{ZtBridge.download(id,document.title.replace(/ - YouTube$/,''),au);}catch(e){}}
function mkdl(){
 dl=el('div','position:fixed;right:12px;bottom:20px;z-index:2147483646;padding:10px 14px;border-radius:18px;background:rgba(74,68,88,.95);color:#e8def8;font:600 14px sans-serif;display:none;box-shadow:0 2px 10px rgba(0,0,0,.5);','\u2B07 Unduh');
 dm=el('div','position:fixed;right:12px;bottom:68px;z-index:2147483647;padding:6px;border-radius:16px;background:#2b2930;color:#e6e0e9;font:15px sans-serif;display:none;box-shadow:0 4px 16px rgba(0,0,0,.6);');
 var a=el('div','padding:10px 14px;','Video (MP4)'),b=el('div','padding:10px 14px;','Audio saja');
 a.addEventListener('click',function(e){e.stopPropagation();go(false);});
 b.addEventListener('click',function(e){e.stopPropagation();go(true);});
 dm.appendChild(a);dm.appendChild(b);
 dl.addEventListener('click',function(e){e.stopPropagation();dm.style.display=dm.style.display==='none'?'block':'none';});
 document.body.appendChild(dl);document.body.appendChild(dm);}
function dlShow(){
 if(!dl||!dl.isConnected||!dm||!dm.isConnected)mkdl();
 var on=cfg.dl&&vid()&&!document.fullscreenElement;
 dl.style.display=on?'block':'none';if(!on)dm.style.display='none';}

var chip=null,menu=null;
function upd(){if(chip)chip.textContent='\u2699 '+lbl(cfg.qs);}
function mkchip(){
 chip=el('div','position:fixed;top:92px;right:10px;z-index:2147483646;padding:7px 12px;border-radius:16px;background:rgba(43,41,48,.85);color:#e6e0e9;font:600 13px sans-serif;display:none;');
 chip.addEventListener('click',function(ev){ev.stopPropagation();menu.style.display=menu.style.display==='none'?'block':'none';});
 menu=el('div','position:fixed;top:132px;right:10px;z-index:2147483647;min-width:130px;padding:6px;border-radius:16px;background:#2b2930;color:#e6e0e9;font:15px sans-serif;display:none;box-shadow:0 4px 16px rgba(0,0,0,.6);');
 for(var i=0;i<QS.length;i++){(function(it){
  var r=el('div','padding:10px 14px;border-radius:10px;',it[0]);
  r.addEventListener('click',function(ev){ev.stopPropagation();set('qs',it[1]);menu.style.display='none';});
  menu.appendChild(r);})(QS[i]);}
 document.body.appendChild(chip);document.body.appendChild(menu);}

var pn=null;
function sec(t){
 var w=el('div','display:flex;align-items:center;padding:22px 4px 8px;');
 w.appendChild(el('div','width:3px;height:16px;border-radius:2px;background:#d0bcff;margin-right:8px;'));
 w.appendChild(el('div','font:600 14px sans-serif;color:#cac4d0;',t));
 return w;}
function card(ic,label,ctl,onTap){
 var r=el('div','display:flex;align-items:center;padding:14px 18px;background:rgba(255,255,255,.07);');
 if(ic){r.appendChild(el('div','width:40px;height:40px;border-radius:12px;background:rgba(208,188,255,.14);color:#d0bcff;display:flex;align-items:center;justify-content:center;font:20px sans-serif;margin-right:14px;flex:none;',ic));}
 r.appendChild(el('div','font:500 16px sans-serif;color:#e6e0e9;flex:1;padding-right:12px;',label));
 if(ctl)r.appendChild(ctl);
 if(onTap)r.addEventListener('click',onTap);
 return r;}
function grp(t,cards){
 var g=el('div','');g.appendChild(sec(t));
 var c=el('div','display:flex;flex-direction:column;gap:4px;');
 var n=cards.length;
 for(var i=0;i<n;i++){
  var rad=n===1?'24px':(i===0?'24px 24px 6px 6px':(i===n-1?'6px 6px 24px 24px':'6px'));
  cards[i].style.borderRadius=rad;c.appendChild(cards[i]);}
 g.appendChild(c);return g;}
function row(label,ctl,ic){return card(ic,label,ctl,null);}
function tog(k){
 var t=el('div','width:46px;height:26px;border-radius:13px;position:relative;flex:none;');
 var d=el('div','position:absolute;top:3px;width:20px;height:20px;border-radius:50%;background:#fff;');
 t.appendChild(d);
 function draw(){t.style.background=cfg[k]?'#6750a4':'#49454f';d.style.left=cfg[k]?'23px':'3px';}
 draw();t.addEventListener('click',function(e){e.stopPropagation();set(k,cfg[k]?0:1);draw();});return t;}
function sel(k,opts){
 var s=el('select','background:#2b2930;color:#e6e0e9;border:1px solid #49454f;border-radius:8px;padding:6px;font:14px sans-serif;');
 for(var i=0;i<opts.length;i++){var o=el('option');o.value=opts[i][1];o.textContent=opts[i][0];s.appendChild(o);}
 s.value=String(cfg[k]);
 s.addEventListener('change',function(){set(k,k==='sp'?parseFloat(s.value):s.value);});return s;}
var MS=[['Ikuti tema','default'],['Kaca transparan','transparent'],['Blur','blur'],['Gradien','gradient'],['Hitam pekat','black']];
function chc(label,val){
 var ck=el('div','font:700 18px sans-serif;color:#d0bcff;width:22px;text-align:center;',cfg.ms===val?'\u2713':'');
 return card(null,label,ck,function(){set('ms',val);build();});}
function build(){
 var keep=pn.scrollTop;
 pn.innerHTML='';
 var h=el('div','display:flex;align-items:center;justify-content:space-between;margin-bottom:4px;');
 h.appendChild(el('div','font:700 22px sans-serif;color:#fff;','Pengaturan ZannTube'));
 var x=el('div','font:700 24px sans-serif;color:#cac4d0;padding:6px 10px;','\u2715');
 x.addEventListener('click',panel);h.appendChild(x);pn.appendChild(h);
 pn.appendChild(grp('Pemutar',[
  row('Resolusi video default',sel('qv',QS),'\u25A3'),
  row('Resolusi Shorts',sel('qs',QS),'\u25A4'),
  row('Kecepatan putar default',sel('sp',SP),'\u23E9')]));
 var mc=[];
 for(var i=0;i<MS.length;i++)mc.push(chc(MS[i][0],MS[i][1]));
 pn.appendChild(grp('Latar mini player',mc));
 pn.appendChild(el('div','font:13px sans-serif;color:#938f99;padding:8px 8px 0;','Berlaku untuk mini player kapsul. Blur butuh Android 12 ke atas.'));
 pn.appendChild(grp('Fitur',[
  row('SponsorBlock: lewati sponsor, promosi, intro, outro',tog('sb'),'\u23ED'),
  row('Tombol Unduh di halaman video',tog('dl'),'\u2B07'),
  row('Sembunyikan Shorts di beranda',tog('hs'),'\u25AF'),
  row('Gaya ala Rythim (tab, kartu bulat, latar gelap)',tog('rs'),'\u25A2'),
  row('Blokir iklan video dan beranda (ala ReVanced)',tog('ab'),'\u2298'),
  row('Putar di latar belakang (notifikasi media)',tog('bg'),'\u266B'),
  row('Layar kecil (PiP) saat keluar aplikasi',tog('pp'),'\u29C9')]));
 pn.appendChild(el('div','font:13px sans-serif;color:#938f99;padding:16px 8px;','Iklan video diblokir otomatis. Unduhan disimpan di Download/ZannTube.'));
 pn.scrollTop=keep;}
function panel(){
 if(pn&&pn.isConnected){pn.remove();pn=null;return;}
 pn=el('div','position:fixed;top:0;left:0;right:0;bottom:0;z-index:2147483647;background:#141218;overflow:auto;padding:16px 16px 40px;');
 document.body.appendChild(pn);
 build();}

var tries=0;
function top_(){
 var tb=document.querySelector('ytm-mobile-topbar-renderer');
 if(!tb)return;
 var bs=tb.querySelectorAll('button,a,ytm-button-renderer');
 for(var i=0;i<bs.length;i++){
  var b=bs[i],tx=((b.innerText||'')+' '+(b.getAttribute('aria-label')||'')).trim();
  if(/^(buka|open)\s*(apl|api|aplikasi|app)/i.test(tx)){var t=b.closest('ytm-button-renderer')||b;t.style.display='none';}
 }
 var lg=tb.querySelector('ytm-home-logo')||tb.querySelector('.mobile-topbar-logo')||tb.querySelector('a[href="/"]')||tb.querySelector('a[aria-label*="YouTube"]');
 if(lg){
  var host=lg.closest('.mobile-topbar-logo')||lg;
  host.style.display='none';
  if(!document.getElementById('zt-logo')){
   var w=el('div','display:inline-flex;align-items:center;gap:6px;');w.id='zt-logo';
   var b2=el('span','display:inline-flex;align-items:center;gap:6px;font:700 20px sans-serif;color:#fff;');
   b2.appendChild(el('span','display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,#d0bcff,#9a5fd0);font:13px sans-serif;color:#fff;','\u25B6'));
   b2.appendChild(el('span','','ZannTube'));
   var g=el('span','font:20px sans-serif;color:#d0bcff;padding:4px 8px;','\u2699');
   g.addEventListener('click',function(e){e.stopPropagation();e.preventDefault();panel();});
   w.appendChild(b2);w.appendChild(g);
   host.parentNode.insertBefore(w,host);
  }
 }
}
function gear(){
 var g=document.getElementById('zt-gear');
 var need=!document.getElementById('zt-logo')&&okp()&&++tries>8;
 if(need&&!g){
  g=el('div','position:fixed;left:12px;bottom:20px;z-index:2147483646;width:42px;height:42px;border-radius:50%;background:rgba(74,68,88,.95);color:#e8def8;font:22px sans-serif;display:flex;align-items:center;justify-content:center;','\u2699');
  g.id='zt-gear';g.addEventListener('click',function(e){e.stopPropagation();panel();});document.body.appendChild(g);}
 if(g)g.style.display=(need&&okp())?'flex':'none';}
var tb2=null,tm=null,tbS=null;
function tabsOn(){
 if(!cfg.rs||!okp())return false;
 var p=location.pathname;
 return p==='/'||p.indexOf('/feed/')===0||(p==='/results'&&/sp=EgJAAQ/.test(location.search));}
function tabAct(){
 var p=location.pathname;
 if(p==='/results')return 2;
 if(p.indexOf('/feed/subscriptions')===0)return 1;
 if(p==='/')return 0;
 return -1;}
function rt(){
 var on=tabsOn();
 if(!tbS){tbS=el('style');tbS.id='zt-tabs-css';(document.head||document.documentElement).appendChild(tbS);}
 tbS.textContent=on?'body{padding-top:44px !important;}ytm-feed-filter-chip-bar-renderer{display:none !important;}':'';
 if(!on){if(tb2)tb2.style.display='none';if(tm)tm.style.display='none';return;}
 var bar=document.querySelector('ytm-mobile-topbar-renderer');
 var top=bar?Math.round(bar.getBoundingClientRect().bottom):56;
 if(!tb2||!tb2.isConnected){
  tb2=el('div','position:fixed;left:0;right:0;height:44px;z-index:2147483640;background:#141218;display:flex;align-items:stretch;overflow-x:auto;white-space:nowrap;padding:0 8px;');
  var T=[['Beranda','/?hl=id'],['Subscription','/feed/subscriptions?hl=id'],['Live','/results?search_query=live&sp=EgJAAQ%253D%253D&hl=id'],['Selengkapnya','']];
  for(var i=0;i<T.length;i++){(function(t,ix){
   var b=el('div','display:flex;align-items:center;padding:0 12px;font:500 14px sans-serif;position:relative;flex:none;',t[0]);
   b.setAttribute('data-ix',ix);
   b.addEventListener('click',function(e){e.stopPropagation();
    if(t[1]){if(tm)tm.style.display='none';location.href='https://m.youtube.com'+t[1];}
    else{if(tm)tm.style.display=tm.style.display==='block'?'none':'block';}});
   tb2.appendChild(b);})(T[i],i);}
  document.body.appendChild(tb2);
  tm=el('div','position:fixed;right:10px;z-index:2147483645;min-width:190px;padding:6px;border-radius:16px;background:#2b2930;color:#e6e0e9;font:15px sans-serif;display:none;box-shadow:0 4px 16px rgba(0,0,0,.6);');
  var M=[['Riwayat','/feed/history?hl=id'],['Playlist','/feed/library?hl=id'],['Pengaturan ZannTube','']];
  for(var k=0;k<M.length;k++){(function(m){
   var r=el('div','padding:12px 14px;border-radius:10px;',m[0]);
   r.addEventListener('click',function(e){e.stopPropagation();tm.style.display='none';if(m[1])location.href='https://m.youtube.com'+m[1];else panel();});
   tm.appendChild(r);})(M[k]);}
  document.body.appendChild(tm);}
 tb2.style.display='flex';tb2.style.top=top+'px';tm.style.top=(top+40)+'px';
 var a=tabAct(),ch=tb2.children;
 for(var q=0;q<ch.length;q++){
  var on2=(q===a);
  ch[q].style.color=on2?'#fff':'#cac4d0';
  ch[q].style.boxShadow=on2?'inset 0 -3px 0 #d0bcff':'none';}}
function apply(){hs();rs();rt();ap();upd();spId='';try{ZtBridge.miniStyle(cfg.ms);}catch(e){}}
setInterval(function(){
 if(!document.body)return;
 try{
  if(!chip||!chip.isConnected||!menu||!menu.isConnected)mkchip();
  if(isSh()){chip.style.display='block';upd();}else{chip.style.display='none';menu.style.display='none';}
  top_();gear();hs();rs();rt();ap();spd();sbLoad();dlShow();
 }catch(e){}
},800);
})();"""
