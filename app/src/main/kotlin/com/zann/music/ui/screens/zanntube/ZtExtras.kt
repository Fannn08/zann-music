package com.zann.music.ui.screens.zanntube

const val ZT_EXTRA_JS = """(function(){
if(window.__ztx)return;window.__ztx=1;
var sy=0,dy=0,act=false,ind=null,busy=false;
function se(){return document.scrollingElement||document.documentElement;}
function okp(){var p=location.pathname;return p.indexOf('/watch')!==0&&p.indexOf('/shorts')!==0&&p.indexOf('/live_chat')!==0;}
function mk(){
 if(ind&&ind.isConnected)return ind;
 ind=document.createElement('div');
 ind.style.cssText='position:fixed;top:0;left:50%;width:42px;height:42px;margin-left:-21px;border-radius:50%;background:#2b2930;color:#d0bcff;z-index:2147483647;display:flex;align-items:center;justify-content:center;font:bold 22px sans-serif;transform:translateY(-70px);box-shadow:0 2px 10px rgba(0,0,0,.6);pointer-events:none;';
 ind.textContent='\u21BB';
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
var QS=[['Auto','auto'],['144p','tiny'],['240p','small'],['360p','medium'],['480p','large'],['720p','hd720'],['1080p','hd1080']];
function gq(){try{return localStorage.getItem('zt_q')||'auto';}catch(e){return 'auto';}}
function sq(q){try{localStorage.setItem('zt_q',q);}catch(e){}}
function lbl(q){for(var i=0;i<QS.length;i++)if(QS[i][1]===q)return QS[i][0];return 'Auto';}
function ap(){
 var q=gq();if(q==='auto')return;
 var p=document.querySelector('.html5-video-player')||document.getElementById('movie_player');
 if(!p)return;
 try{
  if(p.getPlaybackQuality&&p.getPlaybackQuality()===q)return;
  if(p.setPlaybackQualityRange)p.setPlaybackQualityRange(q,q);
  if(p.setPlaybackQuality)p.setPlaybackQuality(q);
 }catch(e){}}
var chip=null,menu=null;
function upd(){if(chip)chip.textContent='\u2699 '+lbl(gq());}
function tg(){menu.style.display=menu.style.display==='none'?'block':'none';}
function mkchip(){
 chip=document.createElement('div');
 chip.style.cssText='position:fixed;top:92px;right:10px;z-index:2147483646;padding:7px 12px;border-radius:16px;background:rgba(43,41,48,.85);color:#e6e0e9;font:600 13px sans-serif;display:none;';
 chip.addEventListener('click',function(ev){ev.stopPropagation();tg();});
 document.body.appendChild(chip);
 menu=document.createElement('div');
 menu.style.cssText='position:fixed;top:132px;right:10px;z-index:2147483647;min-width:130px;padding:6px;border-radius:16px;background:#2b2930;color:#e6e0e9;font:15px sans-serif;display:none;box-shadow:0 4px 16px rgba(0,0,0,.6);';
 for(var i=0;i<QS.length;i++){(function(it){
  var r=document.createElement('div');r.textContent=it[0];r.style.cssText='padding:10px 14px;border-radius:10px;';
  r.addEventListener('click',function(ev){ev.stopPropagation();sq(it[1]);menu.style.display='none';ap();upd();});
  menu.appendChild(r);})(QS[i]);}
 document.body.appendChild(menu);}
setInterval(function(){
 if(!document.body)return;
 if(!chip||!chip.isConnected||!menu||!menu.isConnected)mkchip();
 if(location.pathname.indexOf('/shorts')===0){chip.style.display='block';upd();ap();}
 else{chip.style.display='none';menu.style.display='none';}
},800);
})();"""
