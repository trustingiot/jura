define(
	["react", "js/graba/recordings/modal/UploadRecordingModal"],
	function( React, UploadRecordingModal ) {
		return class RecordingsViewActions extends React.Component {
			constructor( props ) {
				super( props );

				this.createModal = this.createModal.bind( this );
			}

			createModal() {
				return React.createElement( UploadRecordingModal, {
					key: 'uploadRecordingModal',
					post: this.props.post
				} );
			}

			createAction() {
				return React.createElement( 'a', { key: 'uploadAction', href: '#', className: 'action' },
					React.createElement( 'i', {
						className: 'fa fa-upload',
						'data-target': '#upload-recording-modal',
						'data-toggle': 'modal'
					} )
				)
			}

			render() {
				return [
					this.createModal(),
					this.createAction()
				];
			}
		}
	}
);