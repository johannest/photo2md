# photo2md

Tool for converting your (mobile phone) photo of documents to mark down text files

# Architecture

* Vaadin 25 PWA web application
* SpringBoot 4
* Java 21

# Planned features

1. Upload form for taking photos with mobile phone camera
2. Server-side photo to MD pipeline with several steps
   * Image preprocessing for making it high contrast black and white photo
   * Image segmentation: identify titles, paragraphs, lists, tables, and image elements
   * Optical character recognition (OCR) all the text elements 
   * Mark down processor
3. Extensive test suite: 
   * test each elements (titles, paragraps etc.) separately
   * tests for different level of challenging photos from easy (scan) ot more challenging ones (real photos)
