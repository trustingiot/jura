define(
	["react", "react-table", "js/graba/recordings/RecordingsActions"],
	function( React, ReactTable, RecordingsActions ) {
		return class RecordingsTable extends React.Component {
			constructor( props ) {
				super( props );

				this.recordings = [];
			}

			formatRecording( recording ) {
				return {
					subscription: recording.subscription,
					recording: recording.topic,
					startTime: new Date( Number( recording.startTime ) ),
					duration: recording.duration
				}
			}

			render() {
				const data = this.props.recordings
					.map(( e ) => this.formatRecording( e ) )
					.sort( function( a, b ) { return b['startTime'] - a['startTime'] } );

				const columns = [{
					width: 200,
					Header: 'Subscription',
					accessor: 'subscription'
				}, {
					width: 450,
					Header: 'Recording',
					accessor: 'recording'
				}, {
					width: 175,
					id: 'startTime',
					Header: 'Start time',
					accessor: data => data.startTime.toLocaleDateString() + ' ' + data.startTime.toLocaleTimeString()
				}, {
					width: 90,
					Header: 'Duration',
					accessor: 'duration',
					Cell: props => React.createElement( 'span', { className: 'number' }, props.value )
				}, {
					id: 'actions',
					Cell: ( { row } ) => React.createElement(
						RecordingsActions, {
							topic: row.recording,
							post: this.props.post,
							index: row._index,
							indices: this.props.selectedRecordings,
							data: data
						} ),
					width: 150
				}];
				return React.createElement( ReactTable.default, {
					className: '-striped -highlight',
					key: 'table',
					data: data,
					columns: columns,
					defaultPageSize: 5,
					showPageSizeOptions: false,
					showRowHover: true,
					getTrProps: ( state, rowInfo ) => {
						return {
							onClick: ( e ) => {
								let array = this.props.selectedRecordings
								let index = array.indexOf( rowInfo.index )
								if ( index > -1 ) {
									array.splice( index, 1 );
								} else {
									array.push( rowInfo.index );
								}
								this.props.setSelectedRecordings( array );
							},
							style: {
								background: ( rowInfo != undefined && this.props.selectedRecordings.indexOf( rowInfo.index ) > -1 ) ? '#777' : 'white',
								color: ( rowInfo != undefined && this.props.selectedRecordings.indexOf( rowInfo.index ) > -1 ) ? 'white' : '#333',
								cursor: ( rowInfo != undefined ) ? 'pointer' : 'unset'
							}
						}
					}
				} )
			}
		};
	} );