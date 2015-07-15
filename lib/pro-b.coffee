ProBView = require './pro-b-view'
StatusView = require './pro-b-status-view'

{CompositeDisposable} = require 'atom'

module.exports = ProB =
  proBView: null
  modalPanel: null
  subscriptions: null
  statusView: null

  activate: (state) ->
    @proBView = new ProBView(state.proBViewState)
    @modalPanel = atom.workspace.addModalPanel(item: @proBView.getElement(), visible: false)

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
    @modalPanel.destroy()
    @subscriptions.dispose()
    @proBView.destroy()
    @statusView.destroy()

  serialize: ->
    proBViewState: @proBView.serialize()

  toggle: ->
    console.log 'ProB was toggled!'

    if @modalPanel.isVisible()
      @modalPanel.hide()
    else
      @modalPanel.show()
