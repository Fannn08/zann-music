package com.zann.music.ui.screens.zanntube

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import java.lang.ref.WeakReference
import kotlin.math.abs

/** Status video yang sedang diputar di WebView ZannTube. Semua dipanggil dari thread utama. */
object ZtBg {
    val pip = mutableStateOf(false)

    private val main = Handler(Looper.getMainLooper())
    private var web: WeakReference<WebView>? = null
    private var act: WeakReference<Activity>? = null
    private var appCtx: Context? = null
    private var listener: Consumer<PictureInPictureModeChangedInfo>? = null
    private var armed = false
    private var baseSec = 0
    private var baseAt = 0L

    var id = ""
    var title = ""
    var channel = ""
    var playing = false
    var durSec = 0
    var rate = 1f

    private tailrec fun findAct(c: Context): Activity? = when (c) {
        is Activity -> c
        is ContextWrapper -> findAct(c.baseContext)
        else -> null
    }

    fun attach(ctx: Context, w: WebView) {
        detachListener()
        appCtx = ctx.applicationContext
        web = WeakReference(w)
        val a = findAct(ctx)
        act = if (a != null) WeakReference(a) else null
        // Renderer tetap penting walau layar tidak terlihat, supaya suara tidak dimatikan sistem.
        w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        if (a is ComponentActivity) {
            val l = Consumer<PictureInPictureModeChangedInfo> { info -> onPip(info.isInPictureInPictureMode) }
            a.addOnPictureInPictureModeChangedListener(l)
            listener = l
        }
    }

    private fun detachListener() {
        val a = act?.get()
        val l = listener
        if (a is ComponentActivity && l != null) a.removeOnPictureInPictureModeChangedListener(l)
        listener = null
    }

    fun detach() {
        hardStop()
        detachListener()
        pip.value = false
        web = null
    }

    fun hardStop() {
        id = ""
        playing = false
        arm(false)
        val c = appCtx
        if (c != null) {
            try { c.stopService(Intent(c, ZtPlayService::class.java)) } catch (e: Exception) { }
        }
    }

    private fun onPip(on: Boolean) {
        pip.value = on
        js("var h=document.documentElement;if(h){h.classList.toggle('ztpip'," + on + ");if(" + on + ")window.scrollTo(0,0);}")
    }

    /** Aktifkan/matikan masuk PiP otomatis saat pengguna menekan Home (Android 12 ke atas). */
    private fun arm(on: Boolean) {
        if (on == armed) return
        armed = on
        val a = act?.get() ?: return
        try {
            val b = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
            if (Build.VERSION.SDK_INT >= 31) b.setAutoEnterEnabled(on)
            a.setPictureInPictureParams(b.build())
        } catch (e: Exception) { }
    }

    fun curPos(): Int {
        val p = if (playing) baseSec + ((SystemClock.elapsedRealtime() - baseAt) * rate / 1000f).toInt() else baseSec
        return if (durSec > 0) p.coerceIn(0, durSec) else p.coerceAtLeast(0)
    }

    fun update(nid: String, pl: Boolean, ti: String, ch: String, pos: Int, dur: Int, r: Float, pipOk: Boolean) {
        if (nid.isEmpty()) {
            if (id.isNotEmpty()) hardStop()
            return
        }
        val drift = nid == id && abs(curPos() - pos) > 2
        val changed = nid != id || pl != playing || ti != title || ch != channel || dur != durSec || r != rate || drift
        id = nid
        playing = pl
        title = ti
        channel = ch
        durSec = dur
        rate = r
        arm(pl && pipOk)
        if (changed) {
            baseSec = pos
            baseAt = SystemClock.elapsedRealtime()
            push()
        }
    }

    private fun push() {
        val s = ZtPlayService.inst
        if (s != null) {
            s.render()
            return
        }
        if (!playing) return
        val c = appCtx ?: return
        try { c.startForegroundService(Intent(c, ZtPlayService::class.java)) } catch (e: Exception) { }
    }

    private fun js(body: String) {
        main.post { web?.get()?.evaluateJavascript("(function(){" + body + "})()", null) }
    }

    fun play() {
        playing = true
        baseAt = SystemClock.elapsedRealtime()
        js("var v=document.querySelector('video');if(v)v.play();")
        ZtPlayService.inst?.render()
    }

    fun pause() {
        baseSec = curPos()
        baseAt = SystemClock.elapsedRealtime()
        playing = false
        js("var v=document.querySelector('video');if(v)v.pause();")
        ZtPlayService.inst?.render()
    }

    fun seekBy(d: Int) {
        baseSec = (curPos() + d).coerceAtLeast(0)
        baseAt = SystemClock.elapsedRealtime()
        js("var v=document.querySelector('video');if(v)v.currentTime=Math.max(0,v.currentTime+(" + d + "));")
        ZtPlayService.inst?.render()
    }

    fun seekTo(ms: Long) {
        baseSec = (ms / 1000L).toInt().coerceAtLeast(0)
        baseAt = SystemClock.elapsedRealtime()
        js("var v=document.querySelector('video');if(v)v.currentTime=" + (ms / 1000L) + ";")
        ZtPlayService.inst?.render()
    }
}

/**
 * Dipasang di setiap halaman: membuat YouTube mengira halaman selalu terlihat (tidak dijeda saat aplikasi di latar belakang)
 * dan melaporkan status video ke ZtBg tiap detik. Bisa dimatikan lewat pengaturan ZannTube.
 */
const val ZT_BG_JS = """(function(){
if(window.__ztbg)return;window.__ztbg=1;
function cf(){try{return JSON.parse(localStorage.getItem('zt_cfg')||'{}');}catch(e){return {};}}
function on(){return cf().bg!==0;}
function pipOn(){return cf().pp!==0;}
function real(n){try{var d=Object.getOwnPropertyDescriptor(Document.prototype,n);return d&&d.get?d.get.call(document):undefined;}catch(e){return undefined;}}
function over(n,fake){try{Object.defineProperty(document,n,{configurable:true,get:function(){return on()?fake:real(n);}});}catch(e){}}
over('hidden',false);over('webkitHidden',false);over('visibilityState','visible');over('webkitVisibilityState','visible');
function blk(e){if(on())e.stopImmediatePropagation();}
['visibilitychange','webkitvisibilitychange'].forEach(function(n){window.addEventListener(n,blk,true);document.addEventListener(n,blk,true);});
try{var st=document.createElement('style');st.textContent='html.ztpip div[style*="position: fixed"]{display:none !important;}html.ztpip body{overflow:hidden !important;}';(document.head||document.documentElement).appendChild(st);}catch(e){}
function vid(){var m=location.search.match(/[?&]v=([A-Za-z0-9_-]{11})/);return(location.pathname.indexOf('/watch')===0&&m)?m[1]:'';}
window.__ztHome=function(){
 var a=document.querySelector('ytm-mobile-topbar-renderer a[href="/"]')||document.querySelector('a[href="/"]')||document.querySelector('a[href="/?hl=id"]');
 if(a){try{a.click();return;}catch(e){}}
 if(history.length>1){history.back();return;}
 location.href='https://m.youtube.com/?hl=id';
};
function noApp(){
 try{
  var tb=document.querySelector('ytm-mobile-topbar-renderer');if(!tb)return;
  var bs=tb.querySelectorAll('a,button,ytm-button-renderer');
  for(var i=0;i<bs.length;i++){
   var b=bs[i];if(b.getAttribute('data-ztna'))continue;
   var tx=((b.innerText||'')+' '+(b.getAttribute('aria-label')||'')).trim();
   if(/^(buka|open)\s*(apl|api|aplikasi|app)/i.test(tx)){var t=b.closest('ytm-button-renderer')||b;t.style.display='none';b.setAttribute('data-ztna','1');}
  }
 }catch(e){}
}
setInterval(noApp,300);
var ds=null,PO={passive:true,capture:true};
function rs(){if(!ds)return;var el=ds.el;ds=null;try{el.style.transition='transform .18s,opacity .18s';el.style.transform='';el.style.opacity='';}catch(e){}}
document.addEventListener('touchstart',function(e){
 ds=null;
 if(!vid()||document.fullscreenElement||e.touches.length!==1)return;
 var v=document.querySelector('video');if(!v)return;
 var r=v.getBoundingClientRect(),t=e.touches[0];
 if(t.clientY<r.top||t.clientY>r.top+r.height*0.8||t.clientX<r.left||t.clientX>r.right)return;
 ds={x:t.clientX,y:t.clientY,el:document.querySelector('.html5-video-player')||v};},PO);
document.addEventListener('touchmove',function(e){
 if(!ds)return;
 var t=e.touches[0],d=t.clientY-ds.y,x=Math.abs(t.clientX-ds.x);
 if(d<=8||x>d*0.8){if(ds.el.style.transform)rs2();return;}
 var k=Math.min(d,260);
 ds.el.style.transition='none';
 ds.el.style.transform='translateY('+(k*0.6)+'px) scale('+(1-k/900)+')';
 ds.el.style.opacity=String(1-k/600);},PO);
function rs2(){if(!ds)return;ds.el.style.transition='transform .18s,opacity .18s';ds.el.style.transform='';ds.el.style.opacity='';}
document.addEventListener('touchend',rs,PO);
document.addEventListener('touchcancel',rs,PO);
var sent='';
setInterval(function(){
 try{
  var id=on()?vid():'';var v=document.querySelector('video');
  if(!id||!v){if(sent!==''){sent='';ZtBridge.bgState('',false,'','',0,0,1,false);}return;}
  var o=document.querySelector('ytm-slim-owner-renderer');
  var ch=((o&&o.innerText)||'').split('\n')[0];
  var ti=document.title.replace(/ - YouTube${'$'}/,'');
  var d=isFinite(v.duration)?Math.floor(v.duration):0;
  ZtBridge.bgState(id,!v.paused&&!v.ended,ti,ch,Math.floor(v.currentTime||0),d,v.playbackRate||1,pipOn());
  sent=id;
 }catch(e){}
},1000);
})();
"""
