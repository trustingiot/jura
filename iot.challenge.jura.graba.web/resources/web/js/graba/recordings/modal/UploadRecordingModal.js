define( ["react"], function( React ) {
	return class UploadRecordingModal extends React.Component {
		constructor( props ) {
			super( props );

			this.inputID = 'uploadRecordingFile';

			this.upload = this.upload.bind( this );
		}

		upload() {
			let file = this[this.inputID].files[0];
			if ( file ) {
				var reader = new FileReader();
				reader.readAsText( file );
				reader.onload = ( event ) => {
					var content = JSON.parse( event.target.result );
					content.cmd = 'upload';
					this.props.post( 'recorder', content );
				}
				this[this.inputID].value = '';
			}
		}



		render() {
			return React.createElement( 'div', { className: 'modal fade', id: 'upload-recording-modal' },
				React.createElement( 'div', { className: 'modal-dialog', },
					React.createElement( 'div', { className: 'modal-content' },
						React.createElement( 'div', { className: 'modal-header' },
							React.createElement( 'h5', { className: 'modal-title' }, 'Upload recording' ),
							React.createElement( 'button', { type: 'button', className: 'close', 'data-dismiss': 'modal' },
								React.createElement( 'span', {}, '\u00D7' )
							)
						),
						React.createElement( 'div', { className: 'modal-body' },
							React.createElement( 'div', { className: 'input-group' },
								React.createElement( 'input', { className: 'form-control', id: this.inputID, type: 'file', 'data-allowed-file-extensions': ['txt'], ref: ( v ) => this[this.inputID] = v } ),
								React.createElement( 'span', { className: 'input-group-btn' },
									React.createElement( 'button', { className: 'btn btn-dark', 'data-dismiss': 'modal', onClick: this.upload },
										React.createElement( 'i', { className: 'fa fa-upload' } )
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