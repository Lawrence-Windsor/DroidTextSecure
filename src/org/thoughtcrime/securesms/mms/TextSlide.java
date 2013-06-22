/** 
 * Copyright (C) 2011 Whisper Systems
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.util.LRUCache;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;

import ws.com.google.android.mms.ContentType;
import ws.com.google.android.mms.pdu.CharacterSets;
import ws.com.google.android.mms.pdu.PduPart;

public class TextSlide extends Slide {
	
  private static final int MAX_CACHE_SIZE = 10;
  private static final Map<Uri, SoftReference<String>> textCache =
      Collections.synchronizedMap(new LRUCache<Uri, SoftReference<String>>(MAX_CACHE_SIZE));

  public TextSlide(Context context, MasterSecret masterSecret, PduPart part) {
    super(context, masterSecret, part);
  }

  public TextSlide(Context context, String message) {
    super(context, getPartForMessage(message));
  }

  @Override
    public boolean hasText() {
    return true;
  }
	
  @Override
  public String getText() {
    try {
      SoftReference<String> reference = textCache.get(part.getDataUri());

      if (reference != null) {
        String cachedText = reference.get();

        if (cachedText != null) {
          return cachedText;
        }
      }


      String text = new String(getPartData(), CharacterSets.getMimeName(part.getCharset()));			
      textCache.put(part.getDataUri(), new SoftReference<String>(text));
			
      return text;
    } catch (UnsupportedEncodingException uee) {
      Log.w("TextSlide", uee);
      return new String(getPartData());
    }
  }
	
  private static PduPart getPartForMessage(String message) {
    PduPart part = new PduPart();

    try {
      part.setData(message.getBytes(CharacterSets.MIMENAME_ISO_8859_1));
            
      if (part.getData().length == 0)
        throw new AssertionError("Part data should not be zero!");
            
    } catch (UnsupportedEncodingException e) {
      Log.w("TextSlide", "ISO_8859_1 must be supported!", e);
      part.setData("Unsupported character set!".getBytes());
    }
        
    part.setCharset(CharacterSets.ISO_8859_1);
    part.setContentType(ContentType.TEXT_PLAIN.getBytes());
    part.setContentId((System.currentTimeMillis()+"").getBytes());
    part.setName(("Text"+System.currentTimeMillis()).getBytes());
        
    return part;
  }
}
