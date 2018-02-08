define( ["react"], function( React ) {
	return class RenameRecordingModal extends React.Component {
		constructor( props ) {
			super( props );

			this.id = this.props.topic.split( 'recording' )[1].substring( 1 );
			this.inputID = 'renameRecordingNewName' + this.id;

			this.rename = this.rename.bind( this );
		}

		rename() {
			let name = this[this.inputID].value;
			if ( name ) {
				this.props.post( 'recorder', {
					cmd: 'rename',
					old: this.props.topic,
					new: name
				} );
				this[this.inputID].value = '';
			}
		}

		render() {
			return React.createElement( 'div', { onClick: ( event ) => { event.stopPropagation() }, className: 'modal fade', id: 'rename-recording-modal-' + this.id },
				React.createElement( 'div', { className: 'modal-dialog', },
					React.createElement( 'div', { className: 'modal-content' },
						React.createElement( 'div', { className: 'modal-header' },
							React.createElement( 'h5', { className: 'modal-title' }, 'Rename recording' ),
							React.createElement( 'button', { type: 'button', className: 'close', 'data-dismiss': 'modal' },
								React.createElement( 'span', {}, '\u00D7' )
							)
						),
						React.createElement( 'div', { className: 'modal-body' },
							React.createElement( 'div', { className: 'input-group' },
								React.createElement( 'input', { className: 'form-control', id: this.inputID, type: 'text', ref: ( v ) => this[this.inputID] = v } ),
								React.createElement( 'span', { className: 'input-group-btn' },
									React.createElement( 'button', { className: 'btn btn-dark', 'data-dismiss': 'modal', onClick: this.rename },
										React.createElement( 'i', { className: 'fa fa-edit' } )
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