define(
	["react", "js/graba/ScheduledEvents", "js/graba/RecordingForm", "js/graba/recordings/Recordings", "js/graba/recordings/RecordingsViewActions"],
	function( React, ScheduledEvents, RecordingForm, Recordings, RecordingsViewActions ) {
		return class Graba extends React.Component {
			constructor( props ) {
				super( props );

				this.createBlock = this.createBlock.bind( this );
			}

			createBlock( title, element, actions = [] ) {
				const key = title.replace( / /g, '' );
				return React.createElement( 'div', { key: key, className: 'graba-block' },
					React.createElement( 'span', {},
						React.createElement( 'h6', {}, title ),
						React.createElement( 'span', { className: 'actions' }, actions )
					),
					React.createElement( element, { post: this.post } )
				);
			}

			post( service, data ) {
				var xhr = new XMLHttpRequest();
				xhr.open( 'POST', 'http://' + location.host + '/graba/' + service, true );
				xhr.setRequestHeader( 'Content-Type', 'application/json' );
				xhr.send( JSON.stringify( data ) );
			}

			render() {
				return React.createElement( 'div', { className: 'graba' },
					this.createBlock( 'Scheduled events', ScheduledEvents ),
					this.createBlock( 'Schedule recording', RecordingForm ),
					this.createBlock( 'Recordings', Recordings, React.createElement( RecordingsViewActions, { post: this.post } ) )
				);
			}
		};
	} );