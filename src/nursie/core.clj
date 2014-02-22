(ns nursie.core
  (:use [endophile.core :only [mp to-clj]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as en])
  (:gen-class))

(defn permalink
  "Returns the permalink for a post."
  [post]
  (if (= (:slug post) "index")
    "/"
    (str "/" (:slug post))))

(defn title
  "Returns post's title (the content of the first H1 in its enlive nodes.)"
  [post]
  (:content (first (en/select (:content post) [:h1]))))

(defn pubdate
  "Returns post's pubdate (stored in a comment in its enlive nodes.)"
  [post]
  (some #(when-let [data (:data %)]
           (second (re-find #"pubdate:\s*(\S+)" data)))
        (:content post)))

(defn datesort
  "Sorts posts in descending pubdate order"
  [posts]
  ;; TODO: filter out posts w/out a pubdate?
  (reverse (sort-by pubdate posts)))

(defn prettydate
  ;; TODO: use a standard date library r/than this horror
  "Turns a date-string in extended ISO8601 format into something 
  readable."
  [date-string]
  (when-let [[_ yyyy mm dd hhmi tz]
             (re-matches
               #"(\d\d\d\d)-(\d\d)-(\d\d)T(\d\d:\d\d)(Z|[+-]\d\d:\d\d)"
               date-string)]
    (format "%s %s %s, %s UTC%s"
            dd
            (nth ["January"    "February"  "March"     "April"
                  "May"        "June"      "July"      "August"
                  "September"  "October"   "November"  "December"]
                 (dec (Integer. mm)))
            yyyy
            hhmi
            (if (#{"Z" "+00:00"} tz) "", tz))))

(defn backlinks
  "Returns those posts that link to uri."
  [uri posts]
  (filter #(seq (en/select (:content %) [[:a (en/attr= :href uri)]]))
          posts))

(defn render
  "Renders post as an HTML document (string), filling in pubdates,
  backlinks, etc., using the file at template-path as a template."
  ;; TODO: configurable name for template source
  [template-path post]
  (let [template
        (en/template (io/file template-path) [post]

          [:main]            
          (en/substitute (:content post))

          [:title]
          (en/prepend (title post))

          ;; add a pubdate if we've got one
          [:h1]
          (if-let [pubdate (pubdate post)]
            (en/after
              (en/html [:time {:datetime pubdate} (prettydate pubdate)]))
            identity)

          ;; no self-linking
          [[:a (en/attr= :href (permalink post))]]
          en/unwrap

          ;; remove entire backlinks bit unless we have some backlinks
          [:.backlinks]
          (when (seq (:backlinks post)) identity)

          [:.backlinks :li]
          (en/clone-for [b (datesort (:backlinks post))]
            [:a] (en/do->
                   (en/set-attr :href (permalink b))
                   (en/content (title b)))))]

    (apply str (template post))))

(defn parse-post
  "Creates a map representing a post from a markdown file, with keys for
  slug (post name as  st will appear in its uri) and content (a seq of
  clojure.xml/enlive-html nodes)."
  ;; TODO: configurable file extension for posts
  [file]
  {:slug (->> file .getName (re-matches #"(.+)\.md") second)
   :content (->> file slurp mp to-clj)})

(defn home
  "Adds a home page to posts, replicating the most recent post and
  inserting a permalink to the post's canonical uri."
  [posts]
  (let [last-post (first (datesort posts))]
    (conj posts
      {:slug "index"
       :backlinks (:backlinks last-post)
       :content (en/transform (:content last-post)
                  ;; TODO: make this bit configurable
                  [:h1] (en/after (en/html [:p.permalink
                                            "Permalink: "
                                            [:a {:href (permalink last-post)}
                                             (title last-post)]])))})))

(defn archive
  "Adds an archive page to posts, listing and linking to all other posts."
  [posts]
  (conj
    posts
    {:slug "archive"
     ;; TODO: configurable page title; maybe use a snippet instead?
     :content (en/html [:h1 "Archive"]
                       [:ul.archive
                        (for [p (datesort posts)]
                          [:li [:a {:href (permalink p)} (title p)]])])}))

(defn make-posts
  "Creates posts (maps) for files using parse-post, and then backlinks them."
  [files] 
  (let [posts (map parse-post files)]
    (->
      (for [p posts]
        (assoc p :backlinks (backlinks (permalink p) posts)))
      archive
      home)))

(defn write-post
  "Creates html file for post in the directory at out-path, using the
  file at template-path as a template."
  [out-path template-path post] 
  (let [fname (str out-path
                   (if (= "/" (permalink post))
                     "/index"
                     (permalink post))
                   ".html")]
    (spit fname (render template-path post))
    fname))

(defn build [proj-path]
  (->> (.listFiles (io/file proj-path))
       (filter #(re-find #"\.md$" (.getName %)))
       make-posts
       ;; TODO: configurable output subdirectory
       (map (partial write-post
                     (str proj-path "/site")
                     (str proj-path "/template.html")))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")) 

;;;; eof
