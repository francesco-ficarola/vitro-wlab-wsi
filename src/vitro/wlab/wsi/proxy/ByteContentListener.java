package vitro.wlab.wsi.proxy;

import java.io.IOException;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;

/**
 * This class is used to consume an entity and get the entity-data as byte-array.
 * The only other class which implements ContentListener is SkipContentListener.
 * SkipContentListener is ignoring all content.
 * Look at Apache HTTP Components Core NIO Framework -> Java-Documentation of SkipContentListener. 
 */

class ByteContentListener implements ContentListener {
    final SimpleInputBuffer input = new SimpleInputBuffer(2048, new HeapByteBufferAllocator());

    public void consumeContent(ContentDecoder decoder, IOControl ioctrl)
            throws IOException {
        input.consumeContent(decoder);
    }

    public void finish() {
        input.reset();
    }

    byte[] getContent() throws IOException {
        byte[] b = new byte[input.length()];
        input.read(b);
        return b;
    }

	@Override
	public void contentAvailable(ContentDecoder decoder, IOControl arg1)
			throws IOException {
		input.consumeContent(decoder);
		
	}

	@Override
	public void finished() {
		input.reset();					
	}
}
