define( ["react", "js/scanner/ScannerOutput"], function( React, ScannerOutput ) {
	return class Scanner extends React.Component {
		constructor( props ) {
			super( props );
			this.state = { events: [] };

			this.startScan = this.startScan.bind( this );
			this.stopScan = this.stopScan.bind( this );
			this.cleanEvents = this.cleanEvents.bind( this );
			this.addEvent = this.addEvent.bind( this );
		}

		componentDidMount() {
			this.startScan();
		}

		startScan() {
			let socket = new WebSocket( 'ws://' + location.host + '/graba/scanner' );
			socket.onopen = this.cleanEvents
			socket.onmessage = this.addEvent
			this.socket = socket;
		}

		cleanEvents() {
			this.setState( { events: [] } );
		}

		addEvent( event ) {
			this.setState( prev => ( {
				events: [...prev.events, event]
			} ) );
		}

		componentWillUnmount() {
			this.stopScan();
		}

		stopScan() {
			if ( this.socket != null ) {
				this.socket.close();
				this.socket = null;
			}
		}

		render() {
			return React.createElement( 'div', { className: 'scanner' },
				React.createElement( ScannerOutput, { events: this.state.events, socket: this.socket } )
			);
		}
	};
} );