define( ["react"], function( React ) {
	return class RecordingForm extends React.Component {
		constructor( props ) {
			super( props );

			this.submitRecording = this.submitRecording.bind( this );
		}

		renderInput( id, label, type, placeholder ) {
			return [
				React.createElement( 'label', { key: id + 'label', htmlFor: id, className: 'col-sm-2 col-form-label' }, label ),
				React.createElement( 'div', { key: id + 'div', className: 'col-sm-3' },
					React.createElement( 'input', { key: id, type: type, className: 'form-control', id: id, placeholder: placeholder, ref: ( v ) => this[id] = v } )
				)
			];
		}

		capitalize( v ) {
			return v[0].toUpperCase() + v.slice( 1 );
		}

		submitRecording() {
			this.props.post( 'recorder', {
				duration: this.duration.value,
				startTime: new Date( this.startTime.value ).getTime().toString()
			} );
		}

		handleSubmit( event ) {
			event.preventDefault();
		}

		render() {
			return React.createElement( 'div', { className: 'container submit-form' },
				React.createElement( 'form', { onSumbit: this.handleSubmit },
					React.createElement( 'div', { className: 'form-group row' },
						this.renderInput( 'startTime', 'Start time', 'datetime-local', '' ),
						this.renderInput( 'duration', 'Duration', 'number', 'seconds' )
					),
					React.createElement( 'button', { type: 'submit', className: 'btn btn-dark btn-block', onClick: this.submitRecording }, 'Schedule' )
				)
			);
		}
	};
} );