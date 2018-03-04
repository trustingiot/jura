define(
	["react", "js/graba/recordings/RecordingsTable"],
	function( React, RecordingsTable ) {
		return class Recordings extends React.Component {
			constructor( props ) {
				super( props );

				this.state = { recordings: [], selectedRecordings: [] };

				this.cleanRecordings = this.cleanRecordings.bind( this );
				this.addRecording = this.addRecording.bind( this );
				this.setSelectedRecordings = this.setSelectedRecordings.bind( this );
			}

			componentDidMount() {
				let socket = new WebSocket( 'ws://' + location.host + '/graba/recordings' );
				socket.onopen = this.cleanRecordings
				socket.onmessage = this.addRecording
				this.socket = socket;
			}

			componentWillUnmount() {
				if ( this.socket != null ) {
					this.socket.close();
					this.socket = null;
				}
			}

			cleanRecordings() {
				this.setState( { recordings: [], selectedRecordings: [] } );
			}

			addRecording( recording ) {
				let data = JSON.parse( recording.data );
				if ( data.startTime === undefined ) {
					this.setState(( prev ) => ( {
						recordings: prev.recordings.filter( recording => recording.topic !== data.topic ),
						selectedRecordings: []
					} ) )
				} else {
					this.setState( prev => ( {
						recordings: [...prev.recordings, data],
						selectedRecordings: []
					} ) );
				}
			}

			setSelectedRecordings( selectedRecordings ) {
				this.setState( { selectedRecordings: selectedRecordings } );
			}

			render() {
				return React.createElement( RecordingsTable, { recordings: this.state.recordings, selectedRecordings: this.state.selectedRecordings, setSelectedRecordings: this.setSelectedRecordings, post: this.props.post } );
			}
		};
	} );