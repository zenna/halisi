(ns relax.render
  (:import (org.lwjgl.opengl Display DisplayMode GL11 GL30 GLContext)
           (org.lwjgl.util.glu GLU)
           (org.lwjgl BufferUtils)))

(use 'relax.graphics)

(set! *warn-on-reflection* true)


;; ======================================================================
;; spinning triangle in OpenGL 1.1
(defn init-window
  [width height title]
  (def globals (ref {:width width
                     :height height
                     :title title
                     :angle 0.0
                     :last-time (System/currentTimeMillis)}))
  (Display/setDisplayMode (DisplayMode. width height))
  (Display/setTitle title)
  (Display/create))

(defn init-gl
  []
  ; (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
  (GL11/glClearColor 0.0 0.0 0.0 0.0)
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GLU/gluOrtho2D 0.0 (:width @globals)
                  0.0 (:height @globals))
  (GL11/glMatrixMode GL11/GL_MODELVIEW))

(defn draw-poly
  "Draws a polygon to the display"
  [poly]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT  GL11/GL_DEPTH_BUFFER_BIT))
  (GL11/glPolygonMode GL11/GL_FRONT_AND_BACK GL11/GL_FILL)
  (GL11/glBegin GL11/GL_POLYGON)
  (GL11/glEdgeFlag true)
  (doseq [vertex poly]
    (GL11/glVertex2f (first vertex) (second vertex)))
  (GL11/glEnd))

(def mem-range
  (memoize (fn [width height]
              (vec (range (* width height))))))

(defn buffer-to-vec
  [^java.nio.ByteBuffer buffer width height bpp]
  (vec (for [x (range width)
        y (range height)
        :let [i (* bpp (+ x (* width y)))]]
    (/ (bit-and (.getInt buffer i) 0xFF) 255))))

(defn draw-poly-standalone
  "Just draw and update"
  [poly]
  (draw-poly poly)
  (Display/update)
  (Display/sync 60))

(defn poly-to-pixels
  "returns pixel data of rendering img of polygon"
  [points width height]
  ; (println "entering poly-to-pixels")
  (let [;pvar (println "Points" points)
        bpp 4
        ^java.nio.ByteBuffer buffer (BufferUtils/createByteBuffer (* width height bpp))]

  (draw-poly points)
  (Display/update)
  (Display/sync 60)
  (GL11/glReadBuffer GL11/GL_FRONT) ; specify colour buffer
  (GL11/glReadPixels 0 0 width height GL11/GL_RGBA GL11/GL_UNSIGNED_BYTE buffer)
  (buffer-to-vec buffer width height bpp)))

(defn close-display
  []
  (Display/destroy))

; (defn print-buffer
;   [buffer-as-vec width height]
;   (doseq [i (range (* width height))]
;     (if (zero? (mod i (* 3 width)))
;       (print "\n" (.getInt buffer i) " ")
;       (print (.getInt buffer i) " "))))))


    ; {:x x :y y :v (/ (bit-and (.get buffer i) 0xFF) 255)}))

; an integer is 
; (defn main
;   []
;   (println "Run example Alpha")
;   (let [width 20
;         height 2
;         buffer (poly-to-pixels (gen-convex-poly width height) width height)
;         buffer-as-vec (buffer-to-vec buffer width height 4)]
;     (print-buffer buffer-as-vec width height)))