define( ["react"], function( React ) {
	return class RenameRecordingModal extends React.Component {
		constructor( props ) {
			super( props );

			this.id = this.props.topic.split( 'recording' )[1].substring( 1 );
			this.inputID = 'aggregateRecordingsName' + this.id;

			this.aggregate = this.aggregate.bind( this );
		}

		aggregate() {
			let name = this[this.inputID].value;
			if ( name ) {
				this.props.post( 'aggregate', {
					a: this.props.data[this.props.indices[0]].recording.split( 'jura' )[1].substring( 1 ),
					b: this.props.data[this.props.indices[1]].recording.split( 'jura' )[1].substring( 1 ),
					publication: 'recording/' + name
				} );
				this[this.inputID].value = '';
			}
		}

		render() {
			return React.createElement( 'div', { onClick: ( event ) => { event.stopPropagation() }, className: 'modal fade', id: 'aggregate-recordings-modal-' + this.id },
				React.createElement( 'div', { className: 'modal-dialog', },
					React.createElement( 'div', { className: 'modal-content' },
						React.createElement( 'div', { className: 'modal-header' },
							React.createElement( 'h5', { className: 'modal-title' }, 'Aggreagate recordings' ),
							React.createElement( 'button', { type: 'button', className: 'close', 'data-dismiss': 'modal' },
								React.createElement( 'span', {}, '\u00D7' )
							)
						),
						React.createElement( 'div', { className: 'modal-body' },
							React.createElement( 'div', { className: 'input-group' },
								React.createElement( 'input', { className: 'form-control', id: this.inputID, type: 'text', ref: ( v ) => this[this.inputID] = v } ),
								React.createElement( 'span', { className: 'input-group-btn' },
									React.createElement( 'button', { className: 'btn btn-dark', 'data-dismiss': 'modal', onClick: this.aggregate },
										React.createElement( 'i', { className: 'fa fa-compress' } )
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