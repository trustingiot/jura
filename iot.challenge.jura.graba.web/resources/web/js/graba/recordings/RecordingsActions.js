define(
	["react", "js/graba/recordings/modal/SchedulePlayerModal", "js/graba/recordings/modal/RenameRecordingModal", "js/graba/recordings/modal/AggregateRecordingsModal"],
	function( React, SchedulePlayerModal, RenameRecordingModal, AggregateRecordingsModal ) {
		return class RecordingsActions extends React.Component {
			constructor( props ) {
				super( props );

				this.id = this.props.topic.split( 'recording' )[1].substring( 1 );

				this.playRecording = this.playRecording.bind( this );
				this.scheduleRecording = this.scheduleRecording.bind( this );
				this.removeRecording = this.removeRecording.bind( this );
				this.createAction = this.createAction.bind( this );
				this.createActions = this.createActions.bind( this );
			}

			playRecording() {
				let date = new Date();
				date.setTime( date.getTime() + 5000 );
				this.scheduleRecording( date );
			}

			scheduleRecording( startTime ) {
				this.props.post( 'player', {
					subscription: this.props.topic.split( 'jura' )[1].substring( 1 ),
					startTime: startTime.getTime().toString()
				} );
			}

			removeRecording() {
				this.props.post( 'recorder', {
					cmd: 'remove',
					topic: this.props.topic
				} );
			}

			aggregatable( rows, row ) {
				return ( rows.length == 2 && rows.indexOf( row ) > -1 );
			}

			createAction( id, fa, f, aProps = {}, iProps = {}, actionable = true ) {
				let ap = Object.assign( { key: id + this.props.topic, id: 'action-' + id + '-' + this.id, href: '#', className: ( actionable ) ? 'action' : 'action deactivated' }, aProps );
				let ip = Object.assign( { key: 'i' + id + this.props.topic, className: 'fa fa-' + fa, onClick: ( event ) => { event.stopPropagation(); f() } }, iProps );

				return React.createElement( 'a', ap, React.createElement( 'i', ip ) );
			}

			createActions() {
				const aggregatable = this.aggregatable( this.props.indices, this.props.index );
				return [
					this.createAction( 'play', 'play', this.playRecording ),
					this.createAction( 'schedule', 'calendar', () => { }, null, { 'data-target': '#schedule-player-modal-' + this.id, 'data-toggle': 'modal' } ),
					this.createAction( 'rename', 'edit', () => { }, null, { 'data-target': '#rename-recording-modal-' + this.id, 'data-toggle': 'modal' } ),
					this.createAction( 'download', 'download', () => { }, { href: this.props.topic.substring( this.props.topic.indexOf( 'jura' ) ) } ),
					this.createAction( 'aggregate', 'compress', () => { }, aggregatable ? { 'data-target': '#aggregate-recordings-modal-' + this.id, 'data-toggle': 'modal' } : null, null, aggregatable ),
					this.createAction( 'trash', 'trash-o', this.removeRecording )]
			}

			createModals() {
				return [
					React.createElement( SchedulePlayerModal, { key: 'schedulePlayerModal' + this.id, post: this.props.post, topic: this.props.topic } ),
					React.createElement( RenameRecordingModal, { key: 'renameRecordingModal' + this.id, post: this.props.post, topic: this.props.topic } ),
					React.createElement( AggregateRecordingsModal, { key: 'aggregateRecordingsModal' + this.id, post: this.props.post, topic: this.props.topic, data: this.props.data, indices: this.props.indices } )
				];
			}

			render() {
				return [
					this.createModals(),
					this.createActions()
				];
			}
		};
	} );