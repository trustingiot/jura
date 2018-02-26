define( ["react"], function( React ) {
	return class ScheduledEvents extends React.Component {
		constructor( props ) {
			super( props );

			this.state = { player: null, recorder: null, date: new Date() };

			this.clearEvents = this.clearEvents.bind( this );
			this.processEvent = this.processEvent.bind( this );
			this.createField = this.createField.bind( this );
			this.extractEventInfo = this.extractEventInfo.bind( this );
		}

		componentDidMount() {
			// Graba socket
			let socket = new WebSocket( 'ws://' + location.host + '/graba/graba' );
			socket.onopen = this.clearEvents
			socket.onmessage = this.processEvent
			this.socket = socket;

			// Timer
			this.timerID = setInterval(
				() => this.tick(),
				1000
			);
		}

		componentWillUnmount() {
			if ( this.socket != null ) {
				this.socket.close();
				this.socket = null;
				this.setState( { player: null, recorder: null } );
			}

			clearInterval( this.timerID );
		}

		clearEvents() {
			this.setState( { player: null, recorder: null } );
		}

		processEvent( event ) {
			let data = JSON.parse( event.data );
			let service = data.topic.split( '/' ).pop();
			let startTime = data['start.time'];

			if ( startTime === undefined ) {
				this.setState( { [service]: null } );

			} else {
				let date = new Date( Number( data['start.time'] ) );
				let recordingTime = data['recording.time'];
				let duration = ( recordingTime === undefined ) ? -1 : Number( recordingTime )
				this.setState( {
					[service]: {
						service: service,
						date: date,
						duration: duration,
						readable: date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
					},
					date: new Date()
				} );
			}
		}

		createField( service ) {
			return React.createElement( 'div', { className: 'field', key: service },
				React.createElement( 'span', { className: 'field-label' }, service ),
				this.extractEventInfo( service.toLowerCase() )
			)
		}

		extractEventInfo( service ) {
			let event = this.state[service];
			if ( event == null ) {
				return React.createElement( 'span', { className: 'field-value field-value-none' }, 'None' );
			} else {
				let start = ( event.date - this.state.date );
				return React.createElement( 'span', { className: 'field-value' },
					this.generateEventBody( event, start ),
					React.createElement( 'a', { href: '#' },
						React.createElement( 'i', { className: ( start > 0 ) ? 'fa fa-trash-o' : 'fa fa-ban', onClick: ( e ) => this.cancelScheduledEvent( service ) } )
					)
				)

			}
		}

		generateEventBody( event, start ) {
			let result = event.readable;
			let diff = start / 1000;
			let posfix;
			if ( diff > 0 ) {
				posfix = 'starts in ' + diff.toFixed( 0 ) + ' seconds';
			} else {
				posfix = ( event.duration > 0 )
					? 'ends in ' + ( event.duration + diff ).toFixed( 0 ) + ' seconds'
					: 'started ' + ( diff * -1 ).toFixed( 0 ) + ' seconds ago';
			}
			return result + ' (' + posfix + ')';
		}

		cancelScheduledEvent( service ) {
			this.props.post( service, { cmd: 'cancel' } );
		}

		tick() {
			if ( this.state.player != null || this.state.recorder != null ) {
				this.setState( {
					date: new Date()
				} );
			}
		}

		render() {
			return React.createElement( 'div', { className: 'container scheduled-events' },
				this.createField( 'Recorder' ),
				this.createField( 'Player' )
			);
		}
	};
} );