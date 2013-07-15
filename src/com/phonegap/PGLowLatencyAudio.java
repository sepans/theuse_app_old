/*
THIS SOFTWARE IS PROVIDED BY ANDREW TRICE "AS IS" AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL ANDREW TRICE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.phonegap;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.CallbackContext;

/**
 * @author triceam
 *
 */
public class PGLowLatencyAudio extends CordovaPlugin {

	public static final String ERROR_NO_AUDIOID="A reference does not exist for the specified audio id.";
	public static final String ERROR_AUDIOID_EXISTS="A reference already exists for the specified audio id.";
	
	public static final String PRELOAD_FX="preloadFX";
	public static final String PRELOAD_AUDIO="preloadAudio";
	public static final String PLAY="play";
	public static final String STOP="stop";
	public static final String LOOP="loop";
	public static final String UNLOAD="unload";
	public static final String VOLUME="volume";
	
	public static final int DEFAULT_POLYPHONY_VOICES = 15;
	

	private static SoundPool soundPool;
	private static HashMap<String, PGLowLatencyAudioAsset> assetMap; 
	private static HashMap<String, Integer> soundMap; 
	private static HashMap<String, ArrayList<Integer>> streamMap; 
	
	/* (non-Javadoc)
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) 
	{
		boolean result = true;
		initSoundPool();
		
		try 
		{
			String audioID = data.getString(0);
			Log.d(audioID, action);
	
			if ( PRELOAD_FX.equals( action ) ) 
			{
				if ( !soundMap.containsKey(audioID) )
				{
					String assetPath =data.getString(1);
					String fullPath = "www/".concat( assetPath );
					
					AssetManager am = cordova.getActivity().getResources().getAssets();
					AssetFileDescriptor afd = am.openFd(fullPath);
					int assetIntID = soundPool.load(afd, 1);
					soundMap.put( audioID , assetIntID );
				}
				else 
				{
					result = false;
				}
			}
			else if ( PRELOAD_AUDIO.equals( action ) ) 
			{
				if ( !assetMap.containsKey(audioID) )
				{
					String assetPath =data.getString(1);
					int voices;
					if ( data.length() < 2 )
					{
						voices = 0;
					}
					else
					{
						voices = data.getInt(2);
					}
					
					String fullPath = "www/".concat( assetPath );
					
					AssetManager am = cordova.getActivity().getResources().getAssets();
					AssetFileDescriptor afd = am.openFd(fullPath);
					
					PGLowLatencyAudioAsset asset = new PGLowLatencyAudioAsset( afd, voices );
					assetMap.put( audioID , asset );
				}
				else 
				{
					result = false;
				}
			}
			else if ( PLAY.equals( action ) || LOOP.equals( action ) ) 
			{
				if ( assetMap.containsKey(audioID) )
				{
					PGLowLatencyAudioAsset asset = assetMap.get( audioID );
					if ( LOOP.equals( action ) ) 
						asset.loop();
					else
						asset.play();
				}
				else if ( soundMap.containsKey(audioID) )
				{
					int loops = 0;
					if ( LOOP.equals( action ) ) {
						loops = -1;
					}
					
					ArrayList<Integer> streams = streamMap.get( audioID );
					if ( streams == null )
						streams = new ArrayList<Integer>();
					
					int assetIntID = soundMap.get( audioID );
					int streamID = soundPool.play( assetIntID, 1, 1, 1, loops, 1);
					streams.add( streamID );
					streamMap.put( audioID , streams );
				}
				else 
				{
					result = false;
				}
			}
			else if ( STOP.equals( action ) || UNLOAD.equals( action ) ) 
			{
				if ( assetMap.containsKey(audioID) )
				{
					PGLowLatencyAudioAsset asset = assetMap.get( audioID );
					asset.stop();
				}
				else if ( soundMap.containsKey(audioID) )
				{
					ArrayList<Integer> streams = streamMap.get( audioID );
					if ( streams != null )
					{
						for ( int x=0; x< streams.size(); x++)
						soundPool.stop( streams.get(x) );
					}
					streamMap.remove( audioID );
				}
				else 
				{
					result = false;
				}
			}
			
			if ( UNLOAD.equals( action ) ) 
			{
				if ( assetMap.containsKey(audioID) )
				{
					PGLowLatencyAudioAsset asset = assetMap.get( audioID );
					asset.unload();
					assetMap.remove( audioID );
				}
				else if ( soundMap.containsKey(audioID) ){
					//streams unloaded and stopped above
					int assetIntID = soundMap.get( audioID );
					soundMap.remove( audioID );
					soundPool.unload( assetIntID );
				}
				else 
				{
					result = false;
				}
			}
			if ( VOLUME.equals( action ) ) 
			{
				if ( assetMap.containsKey(audioID) )
				{
					PGLowLatencyAudioAsset asset = assetMap.get( audioID );
					asset.volume((float) data.getDouble(1),(float) data.getDouble(2));
				}
				else if ( soundMap.containsKey(audioID) ){
					//streams unloaded and stopped above
					//int assetIntID = soundMap.get( audioID );
				}
				else 
				{
					result = false;
				}
			}		} 
		catch (Exception ex) 
		{
			result = false;
		}
		
		return result;
	}

	private void initSoundPool() 
	{
		if ( soundPool == null ) 
		{
			soundPool = new SoundPool(DEFAULT_POLYPHONY_VOICES, AudioManager.STREAM_MUSIC, 1);
		}
		
		if ( soundMap == null) 
		{
			soundMap = new HashMap<String, Integer>();
		}
		
		if ( streamMap == null) 
		{
			streamMap = new HashMap<String, ArrayList<Integer>>();
		}
		
		if ( assetMap ==null ) 
		{
			assetMap = new HashMap<String, PGLowLatencyAudioAsset>();
		}
	}
}
