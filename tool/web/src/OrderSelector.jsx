import './OrderSelector.css';
import React from 'react';  

class OrderSelector extends React.Component {

  constructor() {
    super();
  }

  selectChanged(e, index) {
    let value = e.target.value;
    let element = {name: value, ascending: false};

    let result = this.props.order;
    if (index == this.props.order.length - 1) {
      result.splice(result.length - 1, 0, element);
    } else {
      result[index].name = value;
    }

    this.props.addOrderToFilter(result);
  }

  delete(index) {
    let result = this.props.order;
    result.splice(index, 1);

    this.setState(prevState => ({ order: result }));
    this.props.addOrderToFilter(this.props.order);
  }

  toggleMode(index) {
    let result = this.props.order;
    result[index].ascending = !result[index].ascending;
    this.setState(prevState => ({ order: result }));
    this.props.addOrderToFilter(this.props.order);
  }

  render() {
    return <div>
        <div className="OrderSelector">
          {
            this.props.order.map((item, i) => {
              return <div className={"inlineBlock " + (i == this.props.order.length - 1 && i != 0 ? "transparent" : "")}>
                  {(i != 0 ? <span className="thenBy">then by</span> : "")}
                  <div className="singleSelect">
                    <select value={this.props.order[i].name} onChange={(e) => this.selectChanged(e, i)} >
                      <option value="" disabled selected={this.props.order[i].name == ""} hidden>Select a column</option>
                      {
                        this.props.filterColumns?.map((column, j) => {
                          return <option selected={this.props.order[i].name == column.name} placeholder="column" value={column.name}>{column.name}</option>                
                        })
                      }
                    </select>
                    <div title={this.props.order[i].ascending ? "smallest first" : "largest first"} onClick={(e) => this.toggleMode(i)} className={"sortMode " + (this.props.order[i].ascending ? "ascending" : "")}>
                      <i className={"glyphicon bi " + (this.props.order[i].ascending ? "bi-sort-up-alt" : "bi-sort-down")} ></i>
                    </div>
                    {
                      (this.props.order.length > 1 ? <a href="#" className="deleteSorting" onClick={(e) => this.delete(i)}><i className="bi bi-x"></i></a> : "")
                    }
                  </div>
                </div>
            })
          }

        </div>
      </div>;
  }
}

export default OrderSelector;