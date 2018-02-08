define( ["react"], function( React ) {
	return class SchedulePlayerModal extends React.Component {
		constructor( props ) {
			super( props );

			this.id = this.props.topic.split( 'recording' )[1].substring( 1 );
			this.inputID = 'schedulePlayerStartTime' + this.id;

			this.schedule = this.schedule.bind( this );
		}

		schedule() {
			let date = this[this.inputID].value;
			if ( date ) {
				this.props.post( 'player', {
					subscription: this.id,
					startTime: ( new Date( date ) ).getTime().toString()
				} );
			}
		}

		render() {
			return React.createElement( 'div', { onClick: ( event ) => { event.stopPropagation() }, className: 'modal fade', id: 'schedule-player-modal-' + this.id },
				React.createElement( 'div', { className: 'modal-dialog', },
					React.createElement( 'div', { className: 'modal-content' },
						React.createElement( 'div', { className: 'modal-header' },
							React.createElement( 'h5', { className: 'modal-title' }, 'Shedule the playback' ),
							React.createElement( 'button', { type: 'button', className: 'close', 'data-dismiss': 'modal' },
								React.createElement( 'span', {}, '\u00D7' )
							)
						),
						React.createElement( 'div', { className: 'modal-body' },
							React.createElement( 'div', { className: 'input-group' },
								React.createElement( 'input', { className: 'form-control', id: this.inputID, type: 'datetime-local', ref: ( v ) => this[this.inputID] = v } ),
								React.createElement( 'span', { className: 'input-group-btn' },
									React.createElement( 'button', { className: 'btn btn-dark', 'data-dismiss': 'modal', onClick: this.schedule },
										React.createElement( 'i', { className: 'fa fa-calendar' } )
									)
								)
							)
						)
					)
				)
			);
		}
	};
} );