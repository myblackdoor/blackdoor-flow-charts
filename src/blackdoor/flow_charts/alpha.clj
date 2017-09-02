(ns blackdoor.flow-charts.alpha
  (:require [clojure.string :as str]
            [me.raynes.fs :as fs]
            [plumbing.core :refer :all]
            [rhizome.viz :as viz]
            [taoensso.timbre :as log]))

(def flows
  {:public
   {:com/myblackdoor [:blackdoor/member
                      :blackdoor/guest]
    :blackdoor/guest [:patreon/blackdoor
                      :typeform/subscribe]
    :blackdoor/member [:slack/signin
                       :loomio/signin]
    :loomio/signin [:loomio/subgroups]
    :slack/signin [:slack/commands]
    :facebook/myblackdoor [:com/myblackdoor]
    :instagram/myblackdoor [:com/myblackdoor]}
   :subscribe
   {:typeform/subscribe [:emails/subscribed
                         :mailchimp/blackdoor]
    :emails/subscribed [:patreon/blackdoor]}
   :invite
   {:typeform/invite [:emails/invite]
    :emails/invite [:typeform/apply]}
   :apply
   {:typeform/apply [:emails/applied
                     :loomio.subgroups/applications]
    :emails/applied [:patreon/blackdoor]
    :loomio.subgroups/applications [:loomio.subgroups.applications/approve
                                    :loomio.subgroups.applications/reject]
    :loomio.subgroups.applications/approve [:emails/accepted]
    :emails/accepted [:slackpass/signup]
    :loomio.subgroups.applications/reject [:emails/rejected]}
   :signup
   {:slackpass/signup [:emails/signed-up
                       :docs/orientation
                       :loomio/new-user
                       :slack/new-user
                       :trello/new-user
                       :google-drive/new-user]
    :emails/signed-up [:docs/orientation
                       :calendly.schedule/onboarding
                       :patreon/blackdoor]
    :docs/orientation [:calendly.schedule/onboarding
                       :patreon/blackdoor
                       :loomio.subgroups/blackdoor
                       :slack.channels/blackdoor
                       :trello/blackdoor
                       :google-drive/blackdoor]}
   :report
   {:typeform/report [:loomio.subgroups/moderation]}
   :slack
   {:slack/channels [:slack.channels/blackdoor
                     :slack.channels/story-time
                     :slack.channels/health-safety
                     :slack.channels/tech]
    :slack/commands [:slack.commands/appear
                     :slack.commands/invite
                     :slack.commands/report
                     :slack.commands/talk]
    :slack.commands/appear [:appearin/new-video-conference]
    :slack.commands/invite [:typeform/invite]
    :slack.commands/report [:typeform/report]
    :slack.commands/talk [:calendly.schedule/talk]}
   :calendly
   {:calendly/schedule [:calendly.schedule/onboarding
                        :calendly.schedule/talk
                        :calendly.schedule/pleasetrythisathome
                        :calendly.schedule/sspectacularr]}
   :loomio
   {:loomio/subgroups [:loomio.subgroups/blackdoor
                       :loomio.subgroups/applications
                       :loomio.subgroups/moderation
                       :loomio.subgroups/tech]}
   :integrations
   {:loomio/subgroups [:slack.channels/blackdoor]
    :github/changes [:slack.channels/blackdoor]
    :google-drive/blackdoor [:slack.channels/blackdoor]}})

(def alpha
  (let [flattened (apply merge-with (comp vec concat)
                         (vals flows))]
    (->> flattened
         vals
         (mapcat identity)
         (distinct)
         (<- (zipmap (repeat [])))
         (merge-with (comp vec concat) flattened))))

(def dir "graphs")
(fs/delete-dir dir)
(fs/mkdirs dir)

(defn node->key
  [s]
  (-> s
      str
      (str/replace #":" "")
      keyword))

(defn key->parent
  [k]
  (let [[k & ns] (some-> k
                         namespace
                         (str/split #"\.")
                         reverse)]
    (cond
      (seq ns) (keyword (str/join "." ns) k)
      k (keyword k)
      :else nil)))

(def cluster->parent
  (->> (for [k (keys alpha)
             :let [parent (key->parent k)]
             :when parent]
         [k parent])
       (into {})))

(defn top-cluster
  [k]
  (or (cluster->parent (key->parent k))
      (key->parent k)))

(defn viz
  []
  (viz/save-graph
   (keys alpha) alpha
   :filename (str dir "/alpha.png")
   :node->descriptor (partial hash-map :label))
  (viz/save-graph
   (keys alpha) alpha
   :filename (str dir "/alpha-clusters.png")
   :node->descriptor (partial hash-map :label)
   :cluster->descriptor (partial hash-map :label)
   :node->cluster (comp top-cluster node->key))
  (viz/save-graph
   (keys alpha) alpha
   :filename (str dir "/alpha-clusters-parents.png")
   :node->descriptor (partial hash-map :label)
   :cluster->descriptor (partial hash-map :label)
   :node->cluster (comp key->parent node->key)
   :cluster->parent cluster->parent))

(viz)

(comment
  (viz/view-graph (keys alpha) alpha
                  :node->descriptor (fn [n] {:label n}))



  )
