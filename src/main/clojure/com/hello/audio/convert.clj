(ns com.hello.audio.convert
  (:require
    [clojure.java.io :as io])
  (:import
    [com.hello.audio
      ADPCM
      ADPCMEncoder]
    [java.io File]
    [javax.sound.sampled
      AudioFileFormat
      AudioFormat
      AudioFormat$Encoding
      AudioInputStream
      AudioSystem]))

;; ADPCM: http://faculty.salina.k-state.edu/tim/vox/dialogic_adpcm.pdf

;; Original file is 16-bit 8kHz stereo signed PCM
(def original-file (io/file "resources/M1F1-int16-AFsp.wav"))

(def ulaw-file (io/file "resources/output-ulaw.wav"))
;; ... etc for the other files

(def audio-file-format (AudioSystem/getAudioFileFormat original-file))

(defn stream
  "See the documentation for AudioSystem/getAudioInputStream"
  ^AudioInputStream
  ([^File file]
    (AudioSystem/getAudioInputStream file))
  ([^AudioFormat format ^AudioInputStream in-stream]
    (AudioSystem/getAudioInputStream format in-stream)))

(defn convert
  "Convert the `in-file` to the AudioFormat specified by `format` and store in `out-file`."
  [^AudioFormat format ^File in-file ^File out-file]
  (let [in-stream (stream in-file)
        converted-stream (stream format in-stream)
        file-format (AudioSystem/getAudioFileFormat in-file)]
    (AudioSystem/write converted-stream (.getType file-format) out-file)))

;; Same as original file format but 8-bit instead of 16-bit
(def output-format-pcm
  (let [inf (.getFormat audio-file-format)]
    (AudioFormat. (.getSampleRate inf) 8 (.getChannels inf) true (.isBigEndian inf))))

;; 8-bit ULAW encoding
(def output-format-ulaw
  (->> audio-file-format
    .getFormat
    (AudioSystem/getTargetFormats AudioFormat$Encoding/ULAW)
    first))

;; Same as original file format but 16kHz instead of 8kHz
(def upsampled-pcm-format
  (let [inf (.getFormat audio-file-format)]
    (AudioFormat. 16000.0 (.getSampleSizeInBits inf) (.getChannels inf) true false)))

;; Same as upsampled-pcm-format but mono
(def upsampled-pcm-format-mono
  (let [inf (.getFormat audio-file-format)]
    (AudioFormat. 16000.0 (.getSampleSizeInBits inf) 1 true false)))

(defn read-stream
  "Reads an entire AudioInputStream into a byte[]."
  ^bytes [^AudioInputStream stream]
  (loop [chunks []]
    (let [available (.available stream)
          curr-chunk (byte-array available)]
      (if (pos? available)
        (do
          (.read stream curr-chunk)
          (recur (conj chunks curr-chunk)))
        (->> chunks (apply concat) byte-array)))))

(defn encode-adpcm
  "Given a file in the same format as `upsampled-pcm-format-mono`, encode it as
  ADPCM and store it in `out-file`."
  [^File in-file ^File out-file]
  (-> in-file
    stream
    read-stream
    (ADPCMEncoder/encodeToWav out-file)))
