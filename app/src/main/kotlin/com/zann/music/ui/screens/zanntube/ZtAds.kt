package com.zann.music.ui.screens.zanntube

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** Memblokir permintaan jaringan khusus iklan (host iklan dan jalur pelacakan iklan YouTube). */
object ZtAdBlock {
    private val hosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.",
        "imasdk.googleapis.com", "googletagservices.com", "2mdn.net",
    )
    private val paths = listOf("/pagead/", "/api/stats/ads", "/ptracking", "/get_midroll_info")

    fun isAd(u: Uri): Boolean {
        val h = u.host.orEmpty()
        if (hosts.any { h.contains(it) }) return true
        if (h.endsWith("youtube.com") || h.endsWith("google.com")) {
            val p = u.path.orEmpty()
            return paths.any { p.startsWith(it) }
        }
        return false
    }

    fun empty(): WebResourceResponse = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}

/**
 * Seperti ReVanced: data iklan (adPlacements, playerAds, adSlots) dibuang dari respons pemutar YouTube
 * sebelum dipakai, jadi iklan video tidak pernah dimuat. Sisanya (iklan yang lolos) dilewati dengan cepat
 * dan slot iklan di beranda/hasil cari disembunyikan. Bisa dimatikan lewat pengaturan ZannTube.
 */
const val ZT_ADS_JS = """(function(){
if(window.__ztads)return;window.__ztads=1;
var jp=JSON.parse;
function cf(){try{return jp(localStorage.getItem('zt_cfg')||'{}');}catch(e){return {};}}
function on(){return cf().ab!==0;}
var K=['adPlacements','playerAds','adSlots'];
function cl(o,d){
 if(!o||typeof o!=='object'||d>3)return o;
 for(var i=0;i<K.length;i++){if(o[K[i]]!==undefined)o[K[i]]=[];}
 if(o.playerResponse)cl(o.playerResponse,d+1);
 if(o.response)cl(o.response,d+1);
 if(Array.isArray(o)){for(var j=0;j<o.length&&j<5;j++)cl(o[j],d+1);}
 return o;}
JSON.parse=function(){var r=jp.apply(this,arguments);try{if(on())cl(r,0);}catch(e){}return r;};
try{var oj=Response.prototype.json;Response.prototype.json=function(){return oj.apply(this,arguments).then(function(r){try{if(on())cl(r,0);}catch(e){}return r;});};}catch(e){}
try{var ipr=window.ytInitialPlayerResponse;if(ipr&&on())cl(ipr,0);
 Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,get:function(){return ipr;},set:function(x){ipr=on()?cl(x,0):x;}});}catch(e){}
var S1='ytm-promoted-sparkles-web-renderer,ytm-promoted-sparkles-text-search-renderer,ytm-promoted-video-renderer,ytm-compact-promoted-video-renderer,ytm-companion-slot,ytm-companion-ad-renderer,ytm-ad-slot-renderer,ad-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-statement-banner-renderer,ytm-brand-video-singleton-renderer,ytm-banner-promo-renderer,ytm-mealbar-promo-renderer,ytm-upsell-dialog-renderer,ytm-paid-content-overlay-renderer,#player-ads,.ytp-ad-overlay-container,.ytp-ad-module,.video-ads,.ytp-ad-image-overlay,.ytp-ad-text-overlay{display:none !important;}';
var S2='ytm-rich-item-renderer:has(ytm-ad-slot-renderer),ytm-rich-item-renderer:has(ad-slot-renderer),ytm-item-section-renderer:has(ytm-ad-slot-renderer){display:none !important;}';
function sty(id,css,show){
 var s=document.getElementById(id);
 if(show&&!s){s=document.createElement('style');s.id=id;s.textContent=css;(document.head||document.documentElement).appendChild(s);}
 else if(!show&&s)s.remove();}
var sv=null;
function tick(){
 try{
  var ok=on();
  sty('zt-ads1',S1,ok);sty('zt-ads2',S2,ok);
  if(!ok)return;
  var p=document.querySelector('.html5-video-player');
  var ad=p&&(p.classList.contains('ad-showing')||p.classList.contains('ad-interrupting'));
  var v=document.querySelector('video');
  var sk=document.querySelectorAll('.ytp-skip-ad-button,.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-ad-skip-button-container button,.ytp-ad-skip-button-slot button,.ytp-ad-overlay-close-button');
  for(var i=0;i<sk.length;i++){try{sk[i].click();}catch(e){}}
  if(ad&&v){
   if(!sv)sv={m:v.muted,r:v.playbackRate};
   v.muted=true;v.playbackRate=16;
   if(isFinite(v.duration)&&v.duration>0&&v.currentTime<v.duration-0.1)v.currentTime=v.duration-0.05;
  }else if(sv&&v){v.muted=sv.m;v.playbackRate=sv.r;sv=null;}
 }catch(e){}
}
setInterval(tick,150);
})();
"""
