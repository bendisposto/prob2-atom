{$, $$$, View}  = require 'atom-space-pen-views'

module.exports =
class HistoryView extends View

  @content: (params) ->
    @div id: "historyview"

  initialize: (params) ->


  attached: ->
   atom.prob.ui.rendering("historyview","le-view")


  getTitle: ->
    "ProB"
