define( ["react", "js/device/Transaction"], function( React, Transaction ) {
	return class Result extends React.Component {
		constructor( props ) {
			super( props );

			this.renderTitle = this.renderTitle.bind( this );
			this.renderBody = this.renderBody.bind( this );
		}

		renderTitle() {
			var device = this.props.device;
			var title = ( device != null ) ? 'Device ' + device : '';
			var loadingClassName = 'loading loading-' + ( ( this.props.processing == true ) ? 'active' : 'inactive' );
			return React.createElement( 'div', { key: 'title', className: 'deviceResultTitle' },
				React.createElement( 'h6', { key: 'device' }, title ),
				React.createElement( 'div', { key: 'indicator', className: loadingClassName } )
			);
		}

		renderBody() {
			if ( this.props.processing )
				return [];

			var reject = this.props.reject;
			var transactions = this.props.transactions;

			if ( reject == null && transactions == [] )
				return [];

			return React.createElement( 'div', { key: 'result-body', className: 'result-body' },
				( reject !== undefined ) ?
					React.createElement( 'blockquote', { key: 'result', className: 'blockquote' },
						React.createElement( 'p', { key: 'reject', className: 'device-header' }, reject ) )
					: this.renderTransactions( transactions ) );
		}

		renderTransactions( transactions ) {
			return React.createElement( 'div', { key: 'result', className: 'transactions' },
				transactions.map(( transaction ) => React.createElement( Transaction, { key: transaction.body.timestamp, transaction: transaction } ) )
			);
		}

		render() {
			return React.createElement( 'div', { className: 'container device-result' },
				this.renderTitle(),
				this.renderBody()
			);
		}
	};
} );