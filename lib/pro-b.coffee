StatusView = require './pro-b-status-view'
HistoryView = require './prob-history-view'

url = require 'url'

{CompositeDisposable} = require 'atom'

module.exports = ProB =
  subscriptions: null
  statusView: null

  activate: (state) ->
    atom.workspace.addOpener (uri) ->
      try
        {protocol, host, pathname} = url.parse(uri)
      catch error
        return
      return unless protocol is 'prob:'
      try
        pathname = decodeURI(pathname) if pathname
      catch error
        return
      new HistoryView(greeting: "Hi there")

    # Events subscribed to in atom's system can be easily cleaned up with a CompositeDisposable
    @subscriptions = new CompositeDisposable

    # Register command that toggles this view
    @subscriptions.add atom.commands.add 'atom-workspace', 'pro-b:toggle': => @toggle()
    atom.prob = this
    require './prob_ui'

  consumeStatusBar: (statusBar) ->
    @statusView = new StatusView()
    statusBar.addLeftTile(item: @statusView, priority: 200)

  deactivate: ->
    @subscriptions.dispose()
    @statusView.destroy()

  serialize: ->
    proBViewState: @proBView.serialize()

  toggle: ->
    uri = "prob://history/dd549c70-addd-413f-9d9f-2ce28cdc3bde"
    atom.workspace.open(uri, split: 'right', searchAllPanes: true)
