/*
 * This sketch uses a blob detector to get blob information, that is then passed
 * to the blob tracker  
 */
import blobDetection.*;
import blobtracker.*;
import java.util.ArrayList;
/*
 * The fields below are used to create an image with blobs,
 * detect the blobs in the image, and track each of the blobs
 */  
PGraphics          blobImage;
BlobDetection      blobDetector;
BlobTracker        blobTracker;
/*
 * The fields below are used to manage the blob image area
 */  
int      wWidth        = 400;
int      wHeight       = 400; 
int      move          = 0;
/*
 * Setup is called just one to allocate resources for this Processing
 * application
 */  
void setup() {
  colorMode(HSB);
  size(wWidth, wHeight);   
  rectMode(CENTER);
  /* 
   * Create a graphics object that can be used to draw blobs
   */
  blobImage = createGraphics(wWidth, wHeight);
  /*
   * Create a blob detector for use in finding blobs in the image
   */
  blobDetector = new BlobDetection(wWidth, wHeight);
  blobDetector.setPosDiscrimination(false);
  blobDetector.setThreshold(0.01f);
  /*
   * Create a blob tracker for tracking blobs. Configure the blob
   * tracker by setting a few values. Note that a blob detector is
   * not passed to the blob tracker constructor. This means that blob
   * data must be passed to the blob tracker update routine.
   */
  blobTracker = new BlobTracker();
  blobTracker.setDimensions(wWidth, wHeight);
  blobTracker.setBlobFilters(20, 80, 20, 80);
  blobTracker.setSmoothing(0.16f); 
}
/*
 * The draw routine is invoked continuously after the Processing 
 * application is started
 */
void draw() {
  ArrayList<Blob>    activeBlobs;
  ArrayList<Blob>    blobList;
  ArrayList<Blob>    deadBlobs;
  ArrayList<Blob>    newBlobs;
  Integer            newColor;
  /* 
   * Draw each of the blobs in the graphics image
   */    
  blobImage.beginDraw();
  blobImage.background(200);
  blobImage.fill(0); 
  blobImage.ellipse(100 + move, 100 + move, 30, 30);
  blobImage.ellipse(100 + move, 150 + move, 30, 30);
  blobImage.ellipse(100 + move, 200 + move, 30, 30);
  blobImage.ellipse(100 + move, 250 + move, 30, 30);
  move = move + 1;
  blobImage.endDraw();
  blobImage.loadPixels();    
  /*
   * Display the graphics image in the output window
   */
  image(blobImage, 0, 0, width, height);  
  /*
   * Compute a new set of blob data from the updated image. Get each
   * of the blobs from the blob detector and add it to a blob list.
   * Pass the blob list to the blob tracker.
   */
  blobDetector.computeBlobs(blobImage.pixels);     
  blobList = new ArrayList<Blob>();  
  for (int i = 0; i < blobDetector.getBlobNb(); i++) { 
    blobList.add(blobDetector.getBlob(i));
  }
  /*
   * Invoke the blob tracker with the updated pixels from the
   * graphics image. Get new lists of active blobs, dead blobs,
   * and new blobs. Each of these lists may or may nor be empty.
   */      
  activeBlobs = blobTracker.update(blobList);
  deadBlobs = blobTracker.getDeadBlobs();
  newBlobs = blobTracker.getNewBlobs();     
  /*
   * Draw a rectangle around each of the active blobs. Note that the
   * color is obtained from the blob tracker. Note that the liveness
   * value (also obtained from the blob tracker) is used to set the 
   * alpha channel of the stroke.
   */
  int angle = 0;
  for(Blob b : activeBlobs) {
    color     rgbColor = color(angle, 255, 255);
    float     liveness = blobTracker.getLiveness(b);    
    stroke(rgbColor, round(map(liveness, 0, 100, 0, 255)));
    angle += 60;
    fill(0, 0, 0, 0);
    strokeWeight(3);    
    rect(b.x * width, b.y * height, b.w * width, b.h * height);       
  }        
}  
 


