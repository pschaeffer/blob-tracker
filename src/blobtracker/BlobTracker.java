package blobtracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import blobDetection.Blob;
import blobDetection.BlobDetection;
/**
 * The blob tracker class is used to track blobs. The basic idea is that 
 * blobs may move around to some extent, without becoming new blobs. In 
 * other words, just because a blob moves a bit, doesn't mean it should 
 * be treated as a new blob. The blob tracker class provides mechanisms
 * for following blobs around and associating data with each blob. Typically
 * the blob information will be obtained from a blob detector 
 * (such as <a href="http://www.v3ga.net/processing/BlobDetection/">
 * Blob Detection</a>}. However, this is not required and the blob data 
 * can be derived from any source.
 * <p>
 * Any number of instances of this class can be created. Each instance will 
 * act as a separate blob tracker. Of course, at least one instance of this 
 * class must be created. Note that the routines in this class are not 
 * thread-safe and can only be used by one thread at a time.
 * <p>
 * Once an instance of this class is created, it can be updated any number 
 * of times. Each time an instance is updated, the list of active blobs 
 * will be refreshed. As part of the refresh process, new blobs may be 
 * added to the active blob list and existing blobs may be recognized
 * as having moved. Of course, old blobs may have died as well.
 * <p>
 * A blob is recognized as having died if its liveness value goes to or 
 * below zero. If a blob dies, it is removed from the active blob list and 
 * added to the dead blob list. The active blob list, the dead blob list, 
 * and the new blob list are rebuilt each time an update operation is done. 
 * Each of the three lists is only valid until the next update operation is 
 * executed. The blobs in the three lists are also only valid, until the next
 * updated operation is executed.
 * <p>
 * Each blob has a liveness value. A blob is only considered to be alive 
 * if the liveness value of the blob is greater than zero. A blob is dead 
 * if its liveness value is zero or negative. The liveness value is set 
 * when a blob is created (to the liveness add value), and incremented 
 * when an existing blob is found again as part of a refresh operation. 
 * If an existing blob is not found as part of a refresh operation, then 
 * its liveness value is decremented (possibly too zero, but not below zero). 
 * Note that a maximum liveness value is maintained as well. 
 * <p>
 * The three blob liveness values (add, subtract, and maximum) can all be
 * set using the set liveness method. The default values are add (5.0), 
 * subtract (5.0) and maximum (100.0).
 * <p>
 * Each time an existing blob is found in the blob update information, the
 * old blob location and size are combined with the updated blob location
 * and size. This combination is done with the smoothness value. The reason
 * for this approach is to make transitions from one blob location to another
 * blob location relatively smooth. This approach also eliminates jitter where
 * the size and location of a blob can bounce around, between many samples. 
 * <p>
 * The smoothness value determines the relative weights of the existing blob
 * location and size and the updated blob location and size. A value one (1.0)
 * will only use the updated blob location and size. A value of 0.5 will 
 * combine the existing blob location and size and the updated blob location
 * and size equally. The default value is 0.1. 
 * <p>
 * A blob in the updated blob information is recognized as being the same 
 * as an existing blob if it is close enough to the existing blob. In 
 * practice, all of the blobs in the updated blob information are checked
 * to find the updated blob that is closest to an existing blob. If the
 * distance between the existing blob and the blob in the blob update data
 * is less than or equal to the closeness value, then the blob from the
 * updated data is assumed to be the same as the existing blob (with a new
 * location). The default closeness value is 0.1.
 * <p>
 * The list of updated blobs can be optionally filtered. This is not required
 * and is not the default behavior. However, minimum and maximum blob width
 * and height values can be specified. If they are specified, all blobs in
 * the updated blob information are checked for compliance. Blobs that are
 * too large or too small are just ignored. A maximum number of blobs can
 * also be specified. Note that the dimensions of the blob area must be 
 * specified before the blob filtering information is set. There are no
 * default values for any of the blob filtering values.  
 * 
 * @author      Peter Schaeffer <pschaeffer@gmail.com>
 * @version     1.0       
 * @since       2014-09-15
 */   
 /*
  * Blob tracker images may not be square. This effects distance 
  *   calculations.       
  * Floating point blob filters
  * Predict where moving blobs should be for each update operation
  * Two values for smoothing. One for blob center. One for blob width 
  *   and height.
  * Smooth more values.
  * package-info.java for each package
  * Typical use case for blob tracker
  * @Example tags
  * Which processing version 2.2.1? 150? 
  * Which license to specify
  * exports.txt for a library
  * Naming conventions for the library, package, JAR file, class
  * BlobTracker is dependent on BlobDetection 
  */   
public class BlobTracker {
  /*
   * The values below are the maximum values for width and height in pixels
   */
  private static final int        MAXWIDTH  = 10000;
  private static final int        MAXHEIGHT = 10000;
  /*
   * The next member is the list of active blobs. This list is rebuilt each
   * time the blob data is updated. If may well be empty after a blob update
   * is completed.
   */
  private ArrayList<Blob>         activeBlobs;
  /*
   * The next member is the list of existing blobs. Each time the blob list
   * is updated, the new active blob list is rebuilt from the existing blob
   * list. Just before the new active blob list is returned to the caller 
   * of a blob update operation, the existing blob list is replaced with 
   * the new active blob list. 
   * 
   * In other words, at the end of a blob update operation, the new active
   * blob list and the existing blob list will be identical. The new active
   * blob list is passed back to the caller of update blob list (who might
   * change it). The existing blob list is retained for use by the next 
   * blob update operation. 
   */
  private ArrayList<Blob>         existingBlobs;
  /*
   * The next member is the list of dead blobs. This list is rebuilt each
   * time the list of active blobs is updated. If may well be empty after
   * a blob update is completed.
   */
  private ArrayList<Blob>         deadBlobs;
  /*
   * The next member is the list of new blobs. This list is rebuilt each
   * time the list of active blobs is updated. If may well be empty after
   * a blob update is completed.
   */
  private ArrayList<Blob>         newBlobs;
  /*
   * The next member is a map used to associate (map) blobs to blob 
   * extensions. In other words, if we have a blob, this map can be
   * used to find the blob extension for that blob. The key for this
   * map is the blob. The value is the blob extension. 
   * 
   * This map is built (rebuilt) from scratch each time the blob data
   * is updated. At the end of each blob update operation, the new
   * active blob map data is used to replace the existing blob map
   * information. 
   * 
   * In other words, at the end of a blob update operation, the active
   * blob map and the existing blob map will be identical. The active
   * blob map will be cleared at the start of the next blob update
   * operation. The existing blob map will be used to help build the
   * new active blob map.
   */
  private HashMap<Blob, BlobExt>  activeMap;
  private HashMap<Blob, BlobExt>  existingMap;
  /*
   * The list of active blobs can be updated by passing blob update
   * data to an update method in this class. However, a reference to
   * a blob detection instance can also be saved in an instance of this
   * class. If a reference to a blob detection instance is obtained,
   * then a different update method in this class must be used.  
   */
  private BlobDetection       blobDetector;
  private float               closeness = 0.1f;
  private float               liveAdd = 5.0f;
  private float               liveMax = 100f;
  private float               liveSubtract = 5.0f;  
  private float               smoothing = 0.1f;
  private boolean             blobFilterActive = false;
  private boolean             dimensionsSet = false;
  private int                 width;
  private int                 height;
  private int                 filterCountMax; 
  private int                 wFilterIntMin;
  private int                 wFilterIntMax;
  private int                 hFilterIntMin;
  private int                 hFilterIntMax;       
  /**
   * This is the standard constructor for a blob tracker instance. It creates 
   * a default blob tracker than can be used to track blobs. Some values can 
   * be set later as need be. This constructor takes no parameters. It must
   * be used with the update method that takes a list of updated blob data.
   */
  public BlobTracker() {
    activeBlobs = new ArrayList<Blob>(); 
    existingBlobs = new ArrayList<Blob>(); 
    deadBlobs = new ArrayList<Blob>();
    newBlobs = new ArrayList<Blob>();  
    activeMap = new HashMap<Blob, BlobExt>();
    existingMap = new HashMap<Blob, BlobExt>();
    blobDetector = null;
  }  
  /**
   * This is an alternative constructor for a blob tracker instance. It 
   * creates a modified blob tracker than can be used to track blobs. Some 
   * values can be set later as need be. This constructor take a blob 
   * detection instance. It must be used with the update method that 
   * takes an image pixel array. 
   * 
   * @param detector a fully-configured blob detector than be used to obtain 
   * updated blob information
   * @exception      throws NullPointerException if blob detection reference is null
   * @see            BlobDetection
   */
  public BlobTracker(BlobDetection detector) {
    /*
     * Check the values passed by the caller
     */
    if (detector == null) 
      throw new NullPointerException("Null blob detection reference passed " + 
                                     "to blob tracker constructor");
    activeBlobs = new ArrayList<Blob>(); 
    existingBlobs = new ArrayList<Blob>(); 
    deadBlobs = new ArrayList<Blob>();
    newBlobs = new ArrayList<Blob>();  
    activeMap = new HashMap<Blob, BlobExt>();
    existingMap = new HashMap<Blob, BlobExt>();
    blobDetector = detector;
  } 
  /*
   * This method effectively clones the blob passed to it. A new blob
   * is created and each field from the old blob is copied to the new
   * blob. Note that some fields from the old blob are not copied to 
   * the new blob.
   */
  private Blob cloneBlob(Blob oldBlob) {
    Blob      newBlob;
    /*
     * Check the values passed by the caller
     */
    if (oldBlob == null) 
      throw new NullPointerException("Null old Blob reference passed " + 
                                     "to copy blob routine");
    newBlob = new Blob(oldBlob.parent);
    newBlob.id = oldBlob.id;
    newBlob.w = oldBlob.w;
    newBlob.h = oldBlob.h;
    newBlob.x = oldBlob.x;
    newBlob.xMin = oldBlob.xMin;
    newBlob.xMax = oldBlob.xMax;
    newBlob.xGridMin = oldBlob.xGridMin;
    newBlob.xGridMax = oldBlob.xGridMax;
    newBlob.y = oldBlob.y;
    newBlob.yMin = oldBlob.yMin;
    newBlob.yMax = oldBlob.yMax;
    newBlob.yGridMin = oldBlob.yGridMin;
    newBlob.yGridMax = oldBlob.yGridMax;    
    return newBlob;   
  }
  /*
   * Filter a blob list based on blob size minimums and maximums.
   * Just ignore any blob that is too big or too small. Add all 
   * of the acceptable blobs to the output blob list. 
   */
  private ArrayList<Blob> filterBlobs(ArrayList<Blob> inputBlobs) {
    ArrayList<Blob>   outputBlobs = new ArrayList<Blob>();
    int               outCount = 0;
    for(Blob input : inputBlobs) {
      /*
       * Check if we have reached the output blob count limit
       */
      outCount++;
      if (outCount > filterCountMax)
        break;
      /*
       * Check if each blob is too big or too small
       */
      if (input.w * width < wFilterIntMin || 
          input.w * width > wFilterIntMax)
        continue;
      if (input.h * height < hFilterIntMin || 
          input.h * height > hFilterIntMax)
        continue;      
      outputBlobs.add(input);
    }      
    return outputBlobs;
  }  
  /**
   * Get the data associated with a specific blob. Data can be optionally
   * associated with each blob using a set blob data method call. If 
   * data has been associated with a blob, it can be obtained using 
   * this method. If there is no data associated with a blob, then a 
   * null value will be returned. This is not an error condition. 
   * However, if the blob does not exist, an exception will be thrown. 
   * Note that the blob reference passed to this routine can not be null. 
   * <p>
   * Only blobs that were returned by the last call to update should
   * ever be passed to this routine. If any other blob is passed to
   * this routine, the results are unpredictable.
   * <p> 
   * This method will always return an Object or null. However, if the
   * original value was some other Java class (such as Integer) that was
   * cast to Object, then the return value from this method can be cast
   * back to that class.
   * 
   * @param  b  a blob being tracked by this class
   * @return    the Object reference associated with the blob or null
   * @exception throws NullPointerException if blob reference is null
   * @exception throws NoSuchElementException if blob is unknown
   * @see       Object 
   */
  public Object getBlobData(Blob b) {
    BlobExt   blobExt;
    /*
     * Check the values passed by the caller
     */
    if (b == null) 
      throw new NullPointerException("Null Blob reference passed " + 
                                     "to get blob data routine");
    blobExt = activeMap.get(b);
    if (blobExt == null) 
      throw new NoSuchElementException("Unknown blob value passed to " +
                                       "get blob data routine");                                        
    return blobExt.data; 
  }
  /**
   * Get the liveness value associated with a specific blob. Each blob 
   * has a liveness value associated with it in the blob extension data.
   * This routine will report an error (throw an exception) if a null 
   * blob reference is passed to it. This routine will also report an 
   * error (throw an exception) if no blob extension data exists for a 
   * blob.
   * <p>
   * Only blobs that were returned by the last call to update should
   * ever be passed to this routine. If any other blob is passed to
   * this routine, the results are unpredictable. 
   * 
   * @param b a blob being tracked by this class
   * @return    the liveness value of the blob
   * @exception throws NullPointerException if blob reference is null
   * @exception throws NoSuchElementException if blob is unknown
   * @see       float 
   */
  public float getLiveness(Blob b) {
    BlobExt   blobExt;
    /*
     * Check the values passed by the caller
     */
    if (b == null) 
      throw new NullPointerException("Null Blob reference passed " + 
                                     "to get blob liveness routine");
    blobExt = activeMap.get(b);
    if (blobExt == null) 
      throw new NoSuchElementException("Unknown blob value passed to " +
                                       "get blob liveness routine");                                        
    return blobExt.liveness; 
  }
  /**
   * Return the list of active blobs. These are blobs that either previously
   * existed and still exist, and new blobs. In other words, this is the list 
   * of all currently active blobs. This list may (or may not) change each 
   * time the list of currently active blobs is refreshed. This list may or 
   * may not be empty. 
   * <p>
   * The blob list returned by this routine is exactly the same as the blob 
   * list returned by the update method described below. This method will 
   * return exactly the same list each time, until the list of active blobs 
   * is refreshed. 
   * 
   * @return the list of currently active blobs (which may be empty)
   * @exception throws IllegalStateException if active blobs list does not 
   *            exist
   * @see    ArrayList
   */ 
  public ArrayList<Blob> getBlobs() {
    if (activeBlobs == null) 
      throw new IllegalStateException("Null active blobs reference in get blobs routine");
    return activeBlobs;    
  }   
  /**
   * Return the list of deleted blobs. These are blobs that previously 
   * existed and are now dead. They are now dead because their liveness 
   * value fell to or below zero. Of course, as dead blobs they are no 
   * longer included in the active blob list. This list is rebuilt each 
   * time the active blob set is refreshed. That means that this list can 
   * only be obtained and used until the active blob set is refreshed
   * again.
   * <p>
   * In most cases, this list will be empty after the active blob set is 
   * refreshed. This list can be ignored unless the parent application 
   * needs to know about newly dead blobs. For example, the parent 
   * application might need to release resources associated with each 
   * of the dead blobs. 
   * 
   * @return the list of blobs that died in the current update cycle 
   *         (which may be empty) 
   * @exception throws IllegalStateException if dead blobs list does not 
   *            exist
   * @see    ArrayList
   */ 
  public ArrayList<Blob> getDeadBlobs() {
    if (deadBlobs == null) 
      throw new IllegalStateException("Null dead blobs reference in get dead blobs routine");
    return deadBlobs;    
  }
  /*
   * The method below returns the distance between two blobs passed to 
   * this method. The distance is calculated using the standard Pythagorean 
   * theorem. 
   */
  private double getDistance(Blob a, Blob b) {
    /*
     * Check the values passed by the caller
     */
    if (a == null) 
      throw new NullPointerException("Null first Blob reference passed " + 
                                     "to get blob distance routine");
    if (b == null) 
      throw new NullPointerException("Null second Blob reference passed " + 
                                     "to get blob distance routine");
    return Math.pow(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2), 0.5);    
  }
  /**
   * Return the list of new blobs. These are blobs that were just created 
   * as part of an active blob list refresh operation. Since the new blobs 
   * are now active, they will also be included in the active blob list. 
   * This list is rebuilt each time the active blob set is refreshed. That 
   * means that this list can only be obtained and used until the active 
   * blob set is refreshed again.
   * <p>
   * In most cases, this list will be empty after the active blob set is 
   * refreshed. This list can be ignored unless the parent application 
   * needs to know about newly created blobs. For example, the parent 
   * application might need to obtain resources for each of the new blobs. 
   * 
   * @return the list of new blobs for the current update cycle 
   * (which may be empty)
   * @exception throws IllegalStateException if new blobs list does not 
   *            exist
   * @see    ArrayList 
   */ 
  public ArrayList<Blob> getNewBlobs() {
    if (newBlobs == null) 
      throw new IllegalStateException("Null new blobs reference in get new blobs routine");
    return newBlobs;    
  }
  /**
   * Set the data associated with a specific blob. Data can be optionally 
   * associated with each blob using the set blob data method call. 
   * If data has been associated with a blob, it can be obtained using 
   * the get blob data method. The data value passed by the caller to 
   * this routine can be a null value. This is not an error condition.   
   * However, if the blob does not exist, an exception will be thrown.
   * Note that the blob reference passed to this routine can not be null.
   * <p>
   * Only blobs that were returned by the last call to update should
   * ever be passed to this routine. If any other blob is passed to
   * this routine, the results are unpredictable.\
   * <p>
   * This method must be passed a Java Object. However, an instance of
   * any other Java class can be cast to Object for this call. The value
   * returned by get blob data can later be cast back to the original java
   * class.
   * 
   * @param b   a blob being tracked by this class
   * @param o   object reference or null to be associated with the blob
   * @exception throws NullPointerException if blob reference is null
   * @exception throws NoSuchElementException if blob is unknown
   * @see       Object 
   */
  public void setBlobData(Blob b, Object o) {
    BlobExt   blobExt;
    /*
     * Check the values passed by the caller
     */
    if (b == null) 
      throw new NullPointerException("Null Blob reference passed " + 
                                     "to set blob data routine");
    blobExt = activeMap.get(b);
    if (blobExt == null) 
      throw new NoSuchElementException("Unknown blob value passed to " +
                                       "set blob data routine");  
    blobExt.data = o;
    return; 
  } 
  /**
   * Set the minimum and maximum blob width and height in pixels. This
   * method sets up a set of blob filters that include a minimum and
   * maximum width and height for each blob. Any blob that is either
   * too large or too small is ignored. 
   * <p>
   * This call is strictly optional. If no blob filters are created, then
   * all blobs will be used for blob tracking. Note that this method can
   * not be called until the dimensions (width, height) of the blob area 
   * in pixels have been set. Note that this method does not set any limit
   * on the number of blobs that will be tracked.  
   * 
   * @param wMin minimum blob width in pixels
   * @param wMax maximum blob width in pixels
   * @param hMin minimum blob height in pixels
   * @param hMax maximum blob height in pixels
   * @exception  throws UnsupportedOperationException if blob area width and
   *                    height are not already set
   * @exception  throws IllegalArgumentException if any of the width and height
   *             values are invalid               
   * @see        Blob
   */
  public void setBlobFilters(int wMin, int wMax, int hMin, int hMax) {
    setBlobFilters(wMin, wMax, hMin, hMax, Integer.MAX_VALUE);    
  }
  /**
   * Set the minimum and maximum blob width and height in pixels and 
   * the maximum number of blobs. This method sets up a set of blob 
   * filters that include a minimum and maximum width and height for 
   * each blob. Any blob that is either too large or too small is ignored.
   * A maximum limit on the number of blobs is also set with this method. 
   * <p>
   * This call is strictly optional. If no blob filters are created, then
   * all blobs will be used for blob tracking. Note that this method can
   * not be called until the dimensions (width, height) of the blob area 
   * in pixels have been set. Note that this method does impose a limit
   * on the number of blobs that will be tracked.  
   * 
   * @param wMin minimum blob width in pixels
   * @param wMax maximum blob width in pixels
   * @param hMin minimum blob height in pixels
   * @param hMax maximum blob height in pixels
   * @exception  throws UnsupportedOperationException if blob area width and
   *                    height are not already set
   * @exception  throws IllegalArgumentException if any of the width and height
   *             values are invalid or if the maximum blob count value is invalid
   * @see        Blob
   */
  public void setBlobFilters(int wMin, int wMax, int hMin, int hMax, 
                             int max) {
    /*
     * Check if the width and height dimensions have been set yet.
     * Report an error if the width and height are not already known.
     */
    if (!dimensionsSet)
      throw new UnsupportedOperationException("Blob filters in pixels can not be " +
                                              "until width and height dimensions " +
                                              "have been set"); 
    /*
     * Check the values set by the caller
     */
    if (wMin < 0 || wMin > width || wMin > wMax) 
      throw new IllegalArgumentException("Invalid minimum width passed " +
                                         "to set blob filters routine - " +
                                         Integer.toString(wMin));
    if (wMax < 0 || wMax > width) 
      throw new IllegalArgumentException("Invalid maximum width passed " +
                                         "to set blob filters routine - " +
                                         Integer.toString(wMax));
    if (hMin < 0 || hMin > height || hMin > hMax) 
      throw new IllegalArgumentException("Invalid minimum height passed " +
                                         "to set blob filters routine - " +
                                         Integer.toString(hMin));
    if (hMax < 0 || hMax > height) 
      throw new IllegalArgumentException("Invalid maximum height passed " +
                                         "to set blob filters routine - " +
                                         Integer.toString(hMax));
    if (max < 0) 
      throw new IllegalArgumentException("Invalid maximum blob count passed " +
                                         "to set blob filters routine - " +
                                         Integer.toString(max));
    blobFilterActive = true;
    filterCountMax = max;
    wFilterIntMin = wMin;
    wFilterIntMax = wMax;
    hFilterIntMin = hMin;
    hFilterIntMax = hMax;
  }
  /**
   * Set the closeness value. The closeness value is used to determine
   * if a blob in a blob update list, is actually an old blob that has
   * moved to a new location. If the distance from the existing blob
   * to the blob in the blob update data, is less than or equal to the
   * closeness value, then the blob in the blob update data is presumed
   * to be a moved version of the existing blob. 
   * <p>
   * A closeness value of zero means that the blob from the updated blob
   * information must be in exactly the same location as the existing 
   * blob. A closeness value of one (1.0) allow for a very large difference
   * between the blob locations. The valid range for closeness is from 0.0 
   * to 1.0.
   * 
   * @param close the closeness value used to determine if a blob is new or 
   *              if an existing blob has moved 
   * @exception   throws IllegalArgumentException if the closeness value is 
   *              invalid
   * @see   float 
   */
  public void setCloseness(float close) {
    /*
     * Check the values passed by the caller
     */
    if (close < 0.0f || close > 1.0f)
      throw new IllegalArgumentException("Invalid closeness value passed " +
                                         "to set closeness routine - " +
                                         Float.toString(close));
    closeness = close;    
  }
  /**
   * Set the dimensions of the blob tracking area in pixels. These values 
   * only need to be set, if other values are going to be expressed in 
   * pixels. The native blob detection APIs return floating point values
   * in the range of 0.0 to 1.0. The actual width and height are needed
   * to convert the floating point values to pixels. Note that the values
   * provided here are for the blob tracking area, not the standard output
   * display window. These values can not be obtained from the PApplet 
   * because the PApplet will have the correct information for the standard
   * output window, not the blob tracking area.
   * 
   * @param w the width of the blob tracking area in pixels
   * @param h the height of the blob tracking area in pixels
   * @exception   throws IllegalArgumentException if the blob are width and
   *              height dimensions are invalid
   */
  public void setDimensions(int w, int h) {
    /*
     * Check the values passed by the caller
     */
    if (w <= 0 || w > BlobTracker.MAXWIDTH)
      throw new IllegalArgumentException("Invalid width value passed " +
                                         "to set dimensions routine - " +
                                         Integer.toString(w));
    if (h <= 0 || h > BlobTracker.MAXHEIGHT)
      throw new IllegalArgumentException("Invalid height value passed " +
                                         "to set dimensions routine - " +
                                         Integer.toString(w));
    
    width = w;
    height = h;
    dimensionsSet = true;   
  }
  /**
   * Set the liveness values. The liveness values are used to maintain
   * the liveness value for each blob. There are three liveness values.
   * They are liveness add, liveness subtract, and liveness maximum.
   * <p>
   * When a blob is newly created, the initial liveness value is set
   * to the add value (or the maximum value, whichever is less). Each
   * time the blob data is updated, each blob is checked to see if it
   * can still be found in the updated blob data. 
   * <p>
   * If the existing blob is found in the blob update data, then the
   * liveness value is incremented by the add value. If the updated
   * liveness value exceeds the maximum value, the maximum value is
   * used instead. 
   * <p>
   * If the existing blob is not found in the blob update data, then
   * the liveness value is decremented using the subtract value. If
   * the result of the subtraction is less than zero, zero is used
   * instead. If the updated liveness value is zero, then the existing
   * blob is now presumed to be dead.
   * <p> 
   * All of the liveness values can be zero or greater.   
   *
   * @param add      the initial liveness value of a blob and the amount to be
   *                 added when an existing blob is recognized again 
   * @param subtract the liveness value to be subtracted when an existing blob
   *                 is not recognized in the blob update data
   * @param maximum  the maximum liveness value of a blob
   * @exception      throws IllegalArgumentException if any of the liveness values
   *                 are invalid 
   */
  public void setLiveness(float add, float subtract, float maximum) {
    /*
     * Check the values passed by the caller
     */
    if (add < 0.0f)
      throw new IllegalArgumentException("Invalid liveness add value passed " +
                                         "to set liveness routine - " +
                                         Float.toString(add));
    if (subtract < 0.0f)
      throw new IllegalArgumentException("Invalid liveness subtract value passed " +
                                         "to set liveness routine - " +
                                         Float.toString(subtract));
    if (maximum < 0.0f)
      throw new IllegalArgumentException("Invalid liveness maximum value passed " +
                                         "to set liveness routine - " +
                                         Float.toString(maximum));
    liveAdd = add;
    liveSubtract = subtract;
    liveMax = maximum;
  }
  /**
   * Set the smoothing value. The smoothing value is used to combine
   * old and new blob location and size data. A smoothing value of 1.0
   * means that only the new blob location and size data should be used.
   * A smoothing value of zero means that only the old blob location and
   * size data should be used (not very useful). A smoothing value of 0.1
   * means that 90% of the old location and size data should be used with
   * 10% of the new location and size data. The valid range for smoothing
   * is from 0.0 to 1.0.
   * 
   * @param smooth the smoothness value used to combine existing blob data 
   *               with updated blob data
   * @exception    throws IllegalArgumentException if the smoothness value is
   *               invalid  
   */
  public void setSmoothing(float smooth) {
    /*
     * Check the values passed by the caller
     */
    if (smooth < 0.0f || smooth > 1.0f)
      throw new IllegalArgumentException("Invalid smoothing value passed " +
                                         "to set smoothing routine - " +
                                         Float.toString(smooth));
    smoothing = smooth;    
  }
  /**
   * Update the list of currently active blobs using blob update information
   * obtained from the saved blob detector associated with this blob tracker. 
   * This method merges the blob update information with the existing blob 
   * information to create a new list of currently active blobs. This update 
   * step may result in zero or more blobs dying and zero or more new blobs 
   * being recognized.
   * <p> 
   * All of the newly dead blobs will be added to the dead blob list. The 
   * dead blobs list will remain valid until the update method is called 
   * again. All of the newly created blobs will be added to the new blob 
   * list. The new blobs list will remain valid until the update method 
   * is called again. The active blobs list is rebuilt each time this method
   * is called. The active blobs list will remain valid until the update 
   * method is called again.
   * 
   * @param  pixels an array of image pixels that can be passed to the blob
   *                detector associated with this blob tracker 
   * @return        the updated list of currently active blobs
   * @see           ArrayList
   * @exception     throws NullPointerException if the pixel array reference 
   *                is null
   * @exception     throws NullPointerException if the saved blob detector
   *                reference is null 
   */
  public ArrayList<Blob> update(int pixels[]) {
    ArrayList<Blob>   updatedBlobData; 
    /*
     * Check the values passed by the caller
     */
    if (pixels == null) 
      throw new NullPointerException("Null pixel array reference passed to " + 
                                     "blob update routine");
    /*
     * Make sure that this instance of the blob tracker does have 
     * a blob detector associated with it. This update method is not
     * passed a list of updated blob information by the caller. This
     * method must obtain the updated blob information from the blob
     * detector associated with the blob tracker.
     */
    if (blobDetector == null) 
      throw new NullPointerException("This update method must be used " +
                                     "with a blob tracker that has a blob " +
                                     "detector");    
    blobDetector.computeBlobs(pixels);         
    updatedBlobData = new ArrayList<Blob>();
    for (int i = 0; i < blobDetector.getBlobNb(); i++) { 
      updatedBlobData.add(blobDetector.getBlob(i));
    }
    /*
     * Invoke the blob update implementation
     */
    return updateImpl(updatedBlobData);
  }
  /**
   * Update the list of currently active blobs using blob update information. 
   * This method merges the blob update information with the existing blob 
   * information to create a new list of currently active blobs. This update 
   * step may result in zero or more blobs dying and zero or more new blobs 
   * being recognized.
   * <p> 
   * All of the newly dead blobs will be added to the dead blob list. The 
   * dead blobs list will remain valid until the update method is called 
   * again. All of the newly created blobs will be added to the new blob 
   * list. The new blobs list will remain valid until the update method 
   * is called again. The active blobs list is rebuilt each time this method
   * is called. The active blobs list will remain valid until the update 
   * method is called again.
   *    
   * @param  updatedBlobList a list of updated blob information typically 
   *                         obtained from a blob detector instance
   * @return                 the updated list of currently active blobs
   * @exception              throws NullPointerException if the updated blob list
   *                         reference is null
   * @exception              throws NullPointerException if the saved blob detector
   *                         reference is not null
   * @see                    ArrayList
   */    
  public ArrayList<Blob> update(ArrayList<Blob> updatedBlobList) {
    /*
     * Check the values passed by the caller
     */
    if (updatedBlobList == null) 
      throw new NullPointerException("Null updated blob list passed to " + 
                                     "blob update routine");
    /*
     * Make sure that this instance of the blob tracker does not have 
     * a blob detector associated with it. This update method is passed
     * a list of updated blob information by the caller.
     */
    if (blobDetector != null) 
      throw new NullPointerException("This update method can not be used " +
      		                           "with a blob tracker that has a blob " +
      		                           "detector"); 
    /*
     * Invoke the blob update implementation
     */
    return updateImpl(updatedBlobList);
  }    
  /*
   * Implement the blob update method
   */  
  private ArrayList<Blob> updateImpl(ArrayList<Blob> updatedBlobList) {
    ArrayList<Blob>   updatedData;
    Blob              updatedBlob;  
    BlobExt           existingExt;
    float             curDistance;
    float             live;
    float             minDistance;
    float             smoothingM1 = 1.0f - smoothing;
    int               minIndex;
    /*
     * Check the values passed by the caller
     */
    if (updatedBlobList == null) 
      throw new NullPointerException("Null updated blob list passed to " + 
                                     "blob update implementation routine");
    /*
     * Clear the active, dead, and new blob lists. Also clear the map
     * that associates blobs with blob extensions.
     */
    activeBlobs.clear();
    deadBlobs.clear();
    newBlobs.clear(); 
    activeMap.clear();
    /*
     * Filter the blobs using a set of criteria for minimum and maximum
     * blob sizes
     */    
    if (blobFilterActive)
      updatedBlobList = filterBlobs(updatedBlobList);
    /*
     * Copy the updated blob data into a local array list. This step
     * is required because the array list is changed below and we don't
     * want to modify data passed in by the caller. Note that each of 
     * blobs in the updated blob data is effectively cloned below. This
     * is required because other code changes the blob passed to this
     * routine.
     */
    updatedData = new ArrayList<Blob>();
    for(Blob blobFromList : updatedBlobList) {
      updatedData.add(cloneBlob(blobFromList)); 
    }
    /*
     * Process each of the existing blobs looking for the blob in the update
     * data that is closest to it. Of course, none of blobs in the update data
     * may be that close to an existing blob. This is not an error condition. 
     */
    for(Blob existing : existingBlobs) { 
      /*
       * Get the blob extension data for the old (existing) blob
       */
      existingExt = existingMap.get(existing);
      if (existingExt == null) 
        throw new IllegalStateException("No extension data found " +
        		                            "for existing blob");
      /*
       * Search for the blob in the blob update data that is closest
       * to the existing (old) blob
       */
      minIndex = -1;
      minDistance = Float.MAX_VALUE;
      for(int i = 0; i < updatedData.size(); i++) {
        /*
         * Get the distance from the current existing blob to the blob from 
         * the updated data 
         */
        updatedBlob = updatedData.get(i);
        curDistance = (float) getDistance(existing, updatedBlob);     
        if (curDistance >= minDistance) 
          continue;
        minDistance = curDistance;
        minIndex = i;          
      }    
      /*
       * We may or may not have found a blob in the updated data than can 
       * be matched with an existing blob. If we found a match, then the 
       * liveness value of the existing blob should be incremented. The 
       * matching blob must be removed from the updated data list because 
       * it has now been used. 
       */
      if (minDistance <= closeness) {
        /*
         * Get the blob from the blob update list that was closest to the 
         * existing blob. Add this blob to the new active blob list. 
         */
        if (minIndex < 0) 
          throw new IllegalStateException("Invalid minimum index value " +
          	                            	"in blob update routine");
        updatedBlob = updatedData.get(minIndex);
        /*
         * Reset the location and size information for the current blob.
         * Combine the old location and size information with the new 
         * location and size information using a smoothing factor.
         */
        updatedBlob.x = (updatedBlob.x * smoothing) + 
                        (existing.x * smoothingM1);
        updatedBlob.y = (updatedBlob.y * smoothing) + 
                        (existing.y * smoothingM1);
        updatedBlob.w = (updatedBlob.w * smoothing) + 
                        (existing.w * smoothingM1);
        updatedBlob.h = (updatedBlob.h * smoothing) + 
                        (existing.h * smoothingM1);
        /*
         * Update the liveness value of the blob extension and put
         * the blob into the active blobs list. Note that the blob
         * from the update blob data is used here and not the existing
         * blob. The blob from the update blob data is newer and may not
         * be at the same location. 
         */
        existingExt.addLive(liveAdd, liveMax);
        activeBlobs.add(updatedBlob);
        activeMap.put(updatedBlob, existingExt);
        /*
         * Remove the blob from the blob update data because it has now
         * been used and must not be used again
         */
        updatedData.remove(minIndex);
        continue;        
      }    
      /*
       * If no blob in the updated blob data matched the existing blob, then 
       * the existing blob needs to be updated with a lower liveness value. 
       * The existing blob may die at this point. 
       */
      live = existingExt.subtractLive(liveSubtract);
      if (live <= 0.0f) {
        deadBlobs.add(existing);
        activeMap.put(existing, existingExt);
        continue;       
      }
      /*
       * The existing (old) blob didn't match any of the blobs in the blob
       * update data. However, the existing blob isn't dead either. Just
       * add it to the list of active blobs.
       */
      activeBlobs.add(existing);
      activeMap.put(existing, existingExt);       
    }   
    /*
     * Some of the blobs in the blob update list may not have been used.
     * These blobs are now considered to be new blobs. Add them to the 
     * new blob list and the active blob list.
     */
    for(Blob newBlob : updatedData) {
      activeBlobs.add(newBlob);
      newBlobs.add(newBlob);
      existingExt = new BlobExt(liveAdd, liveMax);
      activeMap.put(newBlob, existingExt);     
    }
    /*
     * We are now ready to return the new active blob list to the caller. 
     * Replace the old existing blob list, with the new active blob list. 
     * Also replace the old (existing) blob map with the new (active) blob 
     * map.
     */
    existingBlobs = new ArrayList<Blob>(activeBlobs);
    existingMap = new HashMap<Blob, BlobExt>(activeMap);
    return activeBlobs;    
  }
}
/*
 * The blob extension class is used to keep track of some additional data 
 * for each actual blob. Really we would like to add these fields to the 
 * Blob class itself. However, that is not possible. This class is used to 
 * achieve the same result. 
 *  
 * This class is only used as part of the implementation of the blob tracker. 
 * This class should not be used for any other purpose, by any other routine. 
 */
final class BlobExt {
  /*
   * The liveness value is used keep track of the status of each blob. 
   * This value is set to an initial value, when a blob is created. This
   * value is incremented each time a blob is found again as part of a
   * blob update. This value is decremented if a blob is not found again
   * as part of a blob update.
   */
  public float    liveness;   
  /*
   * The data field is the additional data associated with each blob.
   * This field can be used for any purpose and can be null. Setting
   * this field to null is not an error condition. In practice, this
   * field will be set to a reference to something other then Object.
   * For example, a Java Integer could be used to keep track of the 
   * color of each blob. The Java Integer will be cast to and from
   * Object.
   */
  public Object   data;
  /* 
   * This is the standard constructor for a blob extension instance. This 
   * constructor creates a blob extension instance with an initial liveness 
   * value. Note that even the initial liveness value is not allowed to 
   * exceed the maximum liveness value.
   */ 
  public BlobExt(float initLive, float max) {
    liveness = initLive; 
    if (liveness > max) 
      liveness = max;
    data = null;
  } 
  /* 
   * The next method increments the liveness value in a blob extension 
   * instance. If the updated liveness value, exceeds the maximum liveness
   * value, then the maximum liveness value is used instead. 
   */
  protected void addLive(float add, float max) {
    liveness += add;
    if (liveness > max) 
      liveness = max;
  }
  /* 
   * The next method decrements the liveness value in a blob extension 
   * instance. If the updated liveness value, falls below zero, then a
   * zero value is used instead.  
   */
  protected float subtractLive(float subtract) {
    liveness -= subtract;
    if (liveness <= 0.0f)
      liveness = 0.0f;
    return liveness;
  }
}