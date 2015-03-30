# Introduction #

The blob tracker class is used to track blobs as they are created, destroyed, and change location in an image.


# Details #

The basic idea is that blobs may move around to some extent, without becoming new blobs. In other words, just because a blob moves a bit, doesn't mean it should be treated as a new blob. The blob tracker class provides mechanisms for following blobs around and associating data with each blob. Typically the blob information will be obtained from a blob detector (such as <a href='http://www.v3ga.net/processing/BlobDetection/'>Blob Detection</a>}. However, this is not required and the blob data can be derived from any source.