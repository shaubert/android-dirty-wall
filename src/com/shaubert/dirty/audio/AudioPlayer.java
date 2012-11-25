package com.shaubert.dirty.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;

public class AudioPlayer {

/*
<div class="post ord" id="p356859">
	<div class="dt feedtype_">
		
		
		
		
		
			<div id="player356859"></div>
			<script type="text/javascript">
				// http%3A%2F%2Fpit.dirty.ru%2Fdirty%2F1%2F2012%2F08%2F13%2F36157-233505-81eb7c79a75dec4b98821b7161dcebae.mp3
				// http%3A%2F%2Fd3.ru%2Fmoney.mp3
				var so = new SWFObject("/flash/audio.swf?d3mode=1&file=%2Fmedia%2F356859&numcomments=16&user=UniTek&numlistened=13424234234&songname=Blind%20Guardian%20-%20Mirror%20Mirror&xmlurl=http%3a%2f%2fd3.ru%2fplayer%2f356859&userpage=%2Fusers%2f36157", "t", "362", "106", "4", "#ffffff");
				// so.addParam("link", "http%3A%2F%2Fdirty.ru");
				// so.addParam("userpic", "http%3A%2F%2Fd3.ru%2Fi%2Fuserpic.gif");
				// so.addParam("userpage", "http%3A%2F%2Fd3.ru%2Fusers%2F5393");
				so.addParam("d3mode", "1");
				so.addParam("file", "%2Fmedia%2F356859");
				so.addParam("numcomments", "16");
				so.addParam("user", "UniTek");
				so.addParam("numlistened", "13424234234");
				so.addParam("songname", "Blind%20Guardian%20-%20Mirror%20Mirror");		
				so.addParam("xmlurl", "http%3a%2f%2fd3.ru%2fplayer%2f356859");		
				so.addParam("userpage", "%2Fusers%2f36157");
				so.addParam("wmode", "transparent");
				so.write("player356859");
			</script>
			Blind Guardian&nbsp;&#151; одна из самых "бородатых" хэви&#150;рок&#150;команд Германии, оказавшая влияние сразу на несколько поколений ценителей тяжелого рока. Их классическое звучание&nbsp;&#151; сочный спид&#150;метал, а поздние вещи сыграны в жанрах пауэр и прогрессив&#150;метал.<br><br>В этом году мужчины выпустили подборку из 3&#150;х дисков, в которой собрали лучшие свои произведения, многие из которых, включая гвоздь программы, были перезаписаны в течении двух последних лет.<br><br>Сочувствующих информирую&nbsp;&#151; подборка зовется "Memories Of A Time To Come". Где купить или скачать&nbsp;&#151; ответит Google.
		
		
		
	</div>
	<div class="dd">
		<a class="c_icon" href="/comments/356859/#comments" onclick="commentsHandler.showBottomCommentForm({'scroll':true}); return false;">&nbsp;</a>
		Написал <a href="/user/UniTek">UniTek</a>, 13.08.2012 в 23.35
		
		
			
				<span>
					| <a href="/comments/356859">16 комментариев</a>
						
				</span> 
			
		
		
		<span class="stars_holder">
			
		</span>
		
		
		
		<div class="vote">
			
			<strong class="vote_result" onclick="voteDetailsHandler.show({type:'post', button:this, id:'356859'});">131</strong>
			
		</div>
	</div>
	
</div>	
 */
	
	public void play() {
		String url = "http://pit.dirty.ru/dirty/1/2012/08/13/36157-233505-81eb7c79a75dec4b98821b7161dcebae.mp3"; // your URL here
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
}
