(ns ui.router
  (:require-macros [reagent.ratom :as ra :refer [reaction]])
  (:require [taoensso.sente  :as sente]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cognitect.transit :as transit]
            [taoensso.encore :as enc  :refer (logf log logp)]))

(def prob (atom nil))
(def id (atom 0))
(defn fresh-id [] (swap! id inc))
(def listeners (atom {}))

(defn deep-merge [left right]
  (let [k (keys right)
        knew (remove #(contains? left %) k)
        kmerge (filter #(contains? left %) k)
        left' (reduce (fn [a e] (assoc a e (get right e))) left knew)]
    (reduce (fn [a e]
              (let [v (get left e)
                    v' (get right e)]
                (if (and (map? v) (map? v'))
                  (assoc a e (deep-merge v v'))
                  (if (= v v') a (assoc a e v'))))) left' kmerge)))

(defn read-transit [db msg]
  (if (:encoding db)
    (let [r (transit/reader (:encoding db))]
      (transit/read r msg))
    (keyword msg)))

(def relay
  (fn [{{send! :send!} :websocket :as db} [t m]]
    (send! [t m])
    db))

(defn decode
  "Takes a handler of 2 arguments, where the second argument is a vector of a
   message type and a message. The message is read through transit if the
   encoding has been set. Otherwise it create a keyword from the message
   (only used when fetching the encoding from the server).
   This middleware should be applied before the with-send middleware."
  [handler]
    (fn [db v]
      (handler db [(first v) (read-transit db (second v))])))

(defn with-send
  "Takes a handler of 2 arguments, where the second argument is a vector of a
   message type, a message and a function that sends a message to the server.
   Should be applied last!"
  [handler]
    (fn [{{send! :send!} :websocket :as db} [type msg]]
      (handler db [type msg send!])))

(rf/register-handler :chsk/encoding relay)

(rf/register-handler
 :sente/encoding
 (comp  decode with-send)
 (fn [db [_ enc send!]]
   (send! [:prob2/handshake {}])
   (assoc db :encoding enc)))

(rf/register-handler
   :de.prob2.kernel/ui-state
   (comp decode)
   (fn [db [_ deltas]]
     (deep-merge db deltas)))

(defn init-websocket []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "127.0.0.1" ":" 3000 "/updates") {:type :auto})]
    (add-watch state :connection-observer (fn [_ _ _ new] (rf/dispatch [:connection-status (:open?  new)])))
    {:chsk chsk
     :ch-chsk ch-recv
     :send! send-fn
     :chsk-state state
     :stop! (sente/start-chsk-router!
             ch-recv
             (fn [e]
               (when (= (:id e) :chsk/recv)
                 (rf/dispatch (vec (:?data e))))))}))

(rf/register-sub
  :trace
  (fn [db [_ uuid]]
    (reaction (get-in @db [:traces uuid]))))

(rf/register-sub
  :trace-list
  (fn [db _]
    (let [traces (reaction (get-in @db [:traces]))
          models (map :model @traces)]
      (logp :trigger)
      (reaction models))))

(rf/register-sub
  :connected
  (fn [db _]
    (reaction (:connected @db))))

(rf/register-handler
  :connection-status
  debug
  (fn [db [_ status]]
    (when status
     (rf/dispatch [:chsk/encoding]))
    (assoc db :connected status :encoding nil)))

(rf/register-handler
    :message
    debug
    (fn [db [_ msg]] (logp :received msg) db))

(rf/register-handler
  :init
  debug
  (fn [db _] (assoc db :websocket (init-websocket))))

(def js-handlers (atom {}))

(defn register-handler [id handler]
  (swap! js-handlers assoc id handler))


(defn ^:extern subscribe
    "Subscribes a callback to a subscription point.

    For instance: We want to trigger the function foo each time the trace with id
    09b2fdfa-f49b-4c5f-be64-ea5e63f0d628 changes:

    de.prob2.subs.subscribe(foo, '[:trace #uuid \"09b2fdfa-f49b-4c5f-be64-ea5e63f0d628\"])
    "
    [id hook & args]
    (let [real-hook (cljs.reader/read-string hook)
          handler (get @js-handlers id)]
            (when handler
              (let [x (rf/subscribe real-hook)]
                (ra/run! (handler (clj->js @x) args))))))


(defn ^:extern register-rf-handler
    "Registers a handler function. The first argument is the handler key,
    the second argument is a function of at least two arguments.
    The first argument is the changed part of the state. The second argument
    is the full state. Additional arguments are those that were passed from
    the subscription."
    [handler f]
    (let [real-handler (cljs.reader/read-string handler)]
      (rf/register-handler
       real-handler
       (fn [db [_ x & args]]
         (logp :handle real-handler)
         (let [jsdb (clj->js db)
               jsx (clj->js x)
               res (apply f jsx jsdb args)]
           db)))))

(defn ^:extern start []
  (logp :atom js/atom)
  (logp :prob (.-prob js/atom))
  (reset! prob (.-prob js/atom))
  (set! (.-ui @prob) ui.router)
  (rf/dispatch [:init]))

(defn le-view []
  [:div "Hallo"])

(defn ^:extern rendering [id view]
  (r/render-component [le-view] (.getElementById js/document id)))

(set! cljs.core/*main-cli-fn* start)
