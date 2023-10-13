import './Filter.css';

import PanelSelector from './PanelSelector.jsx';
import OrderSelector from './OrderSelector.jsx';
import GenotypeSelector from './GenotypeSelector.jsx';

import React from 'react';
import 'bootstrap-icons/font/bootstrap-icons.css';

import Config from './config.js';

class Filter extends React.Component {

	constructor() {
		super();
		this.state = {page: 1, selected: null, columns: null, foundColumns: [], selectedIndex: -1, ops: [], filter: {id: "root", parent: null, basic: false, children: []}, filterSelection: [], order: [{name: "", ascending: false}]};

		this.textareaRef = React.createRef();
		this.fileNameRef = React.createRef();
		this.inputFileRef = React.createRef();
		this.genotypesSelected = [];
		this.genotypes = [];

		this.panelSelectorRef = React.createRef();
	}

	componentDidMount() {
		this.fetchFilter();
	}  

	delay() {
	    return new Promise( resolve => setTimeout(resolve, 2000) );
	}

	fetchFilter() {
		console.log("fetching filters");

		const requestOptions = {
		    method: 'POST',
		    headers: { 'Content-Type': 'application/json' },
		    body: JSON.stringify({ token: this.props.token.tokenString, sample: this.props.sample })
		};

		console.log(requestOptions);

		fetch(Config.apiBaseUrl + '/variant/loadfilter', requestOptions)
		  .then(response => response.json())
		  .then(
		    data => {
		      this.setState(prevState => ({selected: prevState.selected, selectedIndex: prevState.selectedIndex, columns: data, foundColumns: data, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
		    }
		  ).then(() => {
	  		//begin default filter
			this.convertTextToFilter("Variant_quality IN [PASS]");
			this.generateTextview();
			//this.props.apply(this.getJSONFilter());
			//end default filter
		  });
	}	

	/**
	 * called whenever a user selects a filter from the list
	 */
	select(index) {
		this.setState(prevState => ({selected: prevState.foundColumns[index], selectedIndex: index, columns: prevState.columns, foundColumns: prevState.foundColumns, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

	/**
	* called whenever a user types a query in the search bar
	*/
	search(e) {
		let query = e.target.value;
		let result = [];

		for (var i = 0; i < this.state.columns.length; i++) {
			let current = this.state.columns[i];
			if (this.permissiveIncludes(current.name, query) || this.permissiveIncludes(current.link, query)  || this.permissiveIncludes(current.description, query)) {
				result.push(this.state.columns[i])
			}
		}

		let selected = null;

		if (result.length > 0) {
			selected = result[0];
		}

		this.setState(prevState => ({selected: selected, columns: prevState.columns, foundColumns: result, selectedIndex: 0, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

	permissiveIncludes(haystack, needle) {
		if (typeof haystack == 'undefined' || haystack == null) {
			return false;
		}

		return haystack.toLowerCase().includes(needle.toLowerCase());
	}

	/**
	 * returns the filter (from the JSON file)
	 * used to get options in filters with checkboxes
	 */
	getByName(name) {
		if (this.state.columns == null) {
			return null;
		}
		for (var i = 0; i < this.state.columns.length; i++) {
			let current = this.state.columns[i];
			if (current.name.toLowerCase() == name.toLowerCase()) {
				return current;
			}
		}
		return null;
	}

	/**
	 * if a filter (basic or group) is clicked, it is added or removed from filterSelection
	 */
	toggleFilterSelection(filter, e) {
	    e.stopPropagation();
	    if (filter == null) {
	        return;
	    }

		let index = this.state.filterSelection.indexOf(filter);
		if(index !== -1) {
			this.state.filterSelection.splice(index, 1);
		} else {
			this.state.filterSelection.push(filter);
		}

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: this.state.filterSelection, order: prevState.order}));
	}

	/**
	 * if a filter (basic or group) is clicked, it is added or removed from filterSelection
	 */
	unselectAllFilters(e) {
		this.state.filterSelection = [];

        e.stopPropagation();
		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: this.state.filterSelection, order: prevState.order}));
	}

	/**
	 * empties the filter
	 */
	clearFilter() {
		this.genotypes = [];
		this.genotypesSelected = [];
		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: [], filter: {id: "root", parent: null, basic: false, children: []}, filterSelection: [], order: [{name: "", ascending: false}] }));
		this.convertTextToFilter("Variant_quality IN [PASS]");
	}

	/**
	 * finds the LCA (lowest common ancestor) of all elements int he array elements
	 */
	findLowestCommonAncestor(elements) {
		if (elements.length <= 0) {
			return null;
		}

		let sets = []; //a list of sets. Each sets contains the nodes that were visited upwards from each element
		for (var i = 0; i < elements.length; i++) {
			sets.push(new Set());
			let currentElement = elements[i];
			while (currentElement != null) {
				sets[i].add(currentElement);
				currentElement = currentElement.parent;
			}
		}

		//create intersection of sets
		let intersection = sets[0];
		for (var i = 1; i < sets.length; i++) {
			for(var x of intersection) {
				if (!sets[i].has(x)) {
					intersection.delete(x);
				}
			}
		}

		//find lca
		let currentElement = elements[0];
		while (currentElement != null) {
			if (intersection.has(currentElement)) {
				return currentElement;
			}
			currentElement = currentElement.parent;
		}
	}

	/**
	 * computes the euler tour of the filter (tree)
	 * stores the order in which the elements are reached (and thus displayed) in the field 'euler'
	 */
	computeEulerTour() {
		this.eulerNumber = 0;
		this.computeEulerTourRecursive(this.state.filter);
	}

	/**
	 * helper function for the DFS to compute the euler tour
	 */
	computeEulerTourRecursive(filter) {
		if (filter.basic) {
			filter.euler = this.eulerNumber++;
			return;
		}

		for (var i = 0; i < filter.children.length; i++) {
			this.computeEulerTourRecursive(filter.children[i]);
		}
	}

	/**
	 * checks if ancestor is an ancestor of child
	 */
	isAncestor(ancestor, child) {
		if (child.parent == null) {
			return false;
		}

		if (child.parent.id == ancestor.id) {
			return true;
		}

		return this.isAncestor(ancestor, child.parent);
	}

	/**
	 * converts the current filter to text
	 */
	generateTextview() {
		this.opCount = 0;
		let text = this.buildText(this.state.filter);

		if (this.state.order.length > 1) {
			text += " ORDER";
			for (var i = 0; i < this.state.order.length - 1; i++) {
				text += " " + this.state.order[i].name.replaceAll(" ", "_") + " " + (this.state.order[i].ascending ? "ASC" : "DESC");
			}
		}

	  	if (this.genotypes != null && this.genotypes.length > 0) {
	  		let hasGenotypeText = false;
	  		let genotypeText = "";
	  		genotypeText += " GENOTYPES ";
	  		for (var i = 0; i < this.genotypes.length; i++) {
	  			let values = [];
	  			for (var j = 0; j < this.genotypes[i].length; j++) {
	  				values.push(this.genotypes[i][j].value);
	  			}
	  			if (this.genotypes[i].length > 0) {
		  			genotypeText += this.genotypes[i][0].name + " IN [ ";
		  			for (var j = 0; j < values.length; j++) {
		  				genotypeText += values[j] + " ";
		  			}
		  			genotypeText += "] ";
		  			hasGenotypeText = true;
		  		}
	  		}
	  		if (hasGenotypeText) {
	  			text += genotypeText;
	  		}
	  	}

		this.textareaRef.current.value = text;
	}

	/**
	 * converts the passed filter to text
	 */
	buildText(filter) {
		if (filter.basic) {
			this.opCount++;
			let basicString = filter.name.replaceAll(" ", "_") + " " + filter.comparator;

			if (filter.comparator == "IN") {
				basicString += " [ ";
				for (var v of filter.value) {
					basicString += v + " ";
				}
				basicString += "]";
			} else {
				basicString += " " + filter.value;
			}

			return basicString;
		}

		let result = "";
		if (filter.parent != null) {
			result += "(";
		}

		for (var i = 0; i < filter.children.length; i++) {
			if (i > 0) {
				result += " " + this.state.ops[this.opCount - 1] + " ";
			}
			result += this.buildText(filter.children[i]);
		}

		if (filter.parent != null) {
			result += ")";
		}

		return result;
	}

	convertTextToFilterFromEvent(e) {
		this.convertTextToFilter(e.target.value);
	}

	/**
	 * converts the text in the textarea to a filter
	 */
	convertTextToFilter(text) {
		//(Panel = SOME_PANEL OR Gene = BRCA1) AND CAD > 30 AND SIFT > 15 AND Frequency < 0.1 ORDER CAD ASC SIFT DESC GENOTPYES person IN [ 1/1 0/0 ] person2 IN [ 0/0 ]

		//determine order
		let order = [{name: "", ascending: false}];
		let textWithoutOrderOrGenotypes = text;
		if (textWithoutOrderOrGenotypes.includes(" ORDER ")) {
			let orderSplit = textWithoutOrderOrGenotypes.split(" ORDER ");
			textWithoutOrderOrGenotypes = orderSplit[0];
			order = this.getOrderFromText(orderSplit[1].split(" GENOTYPES ")[0]);
		}

		//determine genotypes
		if (text.includes(" GENOTYPES ")) {
			let genotypeSplit = text.split(" GENOTYPES ");
			if (textWithoutOrderOrGenotypes == text) {
				textWithoutOrderOrGenotypes = genotypeSplit[0];
			}
			this.genotypes = this.getGenotypesFromText(genotypeSplit[1]);
		}

		let symbols = textWithoutOrderOrGenotypes.replaceAll("\n", " ").replaceAll("(", " ( ").replaceAll(")", " ) ").replaceAll("[", " [ ").replaceAll("]", " ] ").replaceAll(/\s\s+/g, ' ').trim().split(' ');
		let filter = this.buildFilter(symbols, null, false);
		let ops = [];

		for (var i = 0; i < symbols.length; i++) {
			let symbol = symbols[i];
			if (symbol == "AND" || symbol == "OR") {
				ops.push(symbol);
			}
		}

		console.log(filter);
		
		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: ops, filter: filter, filterSelection: prevState.filterSelection, order: order}));
	}

	getOrderFromText(text) {
		let symbols = text.replaceAll("\n", " ").replaceAll("(", " ( ").replaceAll(")", " ) ").replaceAll("[", " [ ").replaceAll("]", " ] ").replaceAll(/\s\s+/g, ' ').trim().split(' ');
		let order = [];
		for (var i = 0; i < symbols.length; i+=2) {
			order.push({name: symbols[i].replaceAll("_", " "), ascending: symbols[i + 1] == "ASC"});
		}
		order.push({name: "", ascending: false});
		return order;
	}

	getGenotypesFromText(text) {
		let symbols = text.replaceAll("\n", " ").replaceAll("(", " ( ").replaceAll(")", " ) ").replaceAll("[", " [ ").replaceAll("]", " ] ").replaceAll(/\s\s+/g, ' ').trim().split(' ');

		if (symbols.length == 0) {
			return [];
		}

		//format_sm_lkzrz_gt IN [ 0/1 1/0 ] format_sm_lkzrx_gt IN [ 0/1 1/0 1/1 ] 

		let newGenotypes = [];
		let currentName = "";
		let currentGenotypes = [];

		for (var i = 0; i < symbols.length; i++) {
			let symbol = symbols[i];			
			if (currentName == "") {
				currentName = symbol;
			}
			if (symbol == "]") {
				currentName = "";
				newGenotypes.push([...currentGenotypes]);
				currentGenotypes = [];
			} else if (symbol == "0/0" || symbol == "0/1" || symbol == "1/0" || symbol == "1/1") {
				currentGenotypes.push({name: currentName, value: symbol});
			}
		}

		this.genotypesSelected = [];

		for (var i = 0; i < newGenotypes.length; i++) {
			//gtkey as defined in genotypeselector
			let gtkey = 0;
			if (newGenotypes[i].length == 1) {
				if (newGenotypes[i][0] == "1/1") {
					gtkey = 3;
				} else {
					gtkey = 1;
				}
			} else if (newGenotypes[i].length == 2) {
				if (newGenotypes[i][0] == "0/0") {
					gtkey = 6;
				} else {
					gtkey = 2;
				}				
			} else if (newGenotypes[i].length == 3) {
				if (newGenotypes[i][2] == "1/1") {
					gtkey = 4;
				} else {
					gtkey = 5;
				}				
			}

			this.genotypesSelected[newGenotypes[i][0].name?.replaceAll("format_", "").replaceAll("_gt", "")] = gtkey;
		}

		return newGenotypes;
	}	

	/**
	 * creates filter from a list of symbols (e. g. ['(', 'X', '>', '100', 'AND', ...])
	 */
	buildFilter(symbols, parent, basic) {
		let result = {id: Math.random(), parent: parent, basic: basic, children: []};

		if (basic) {
			result.name = symbols[0].replaceAll("_", " ");
			result.comparator = symbols[1];
			result.value = [];

			for (var i = 2; i < symbols.length; i++) {
				result.value.push(symbols[i]);
			}

			//pretty print empty fields
			if (result.comparator == undefined) {
				result.comparator = "?";
			}

			return result;
		}

		let stack = [];
		let buffer = [];
		for (var i = 0; i < symbols.length; i++) {
			let symbol = symbols[i];

			if (symbol == "(") {
				stack.push(symbol);
			}

			console.log(symbol + " sh " + stack.length);

			if (stack.length == 0) {
				if (symbol != "AND" && symbol != "OR") {
					let comparator = symbols[i+1];
					let basicFilter = [symbol, comparator];

					if (comparator == "IN" && symbols[i+2] == "[") {
						i+=3;
						while (symbols[i] != "]" && i < symbols.length) {
							basicFilter.push(symbols[i]);
							console.log(symbols[i] + " in");
							i++;
						}
					} else {
						basicFilter.push(symbols[i+2]);
						i+=2;
					}

					result.children.push(this.buildFilter(basicFilter, result, true));
				}
			} else {
				buffer.push(symbol);
			}

			if (symbol == ")") {
				stack.pop();    
			}

			if (stack.length == 0 && buffer.length != 0) {
				//remove parenthesis in beginning and end
				buffer.splice(0, 1);
				buffer.splice(buffer.length - 1, 1);
				result.children.push(this.buildFilter(buffer, result, false));
				buffer = [];
			}          
		}

		return result;
	}

	getAllEulerValues(filter) {
		if (filter.basic) {
			return [filter.euler - 1];
		}

		let result = [];
		for (var i = 0; i < filter.children.length; i++) {
			result = result.concat(this.getAllEulerValues(filter.children[i]));
		}

		return result;
	}

	/**
	* creates a group from selected elements
	*/
	createGroup() {
		//sort by leftness
		this.computeEulerTour();
		this.state.filterSelection.sort((a, b) => a.euler - b.euler);

		//remove any elements whose ancestors are also in the list (you cannot create a group with your ancestor -> circular)
		let indicesOfElementsWithAncestorsInSelection = [];
		for (var i = 0; i < this.state.filterSelection.length; i++) {
			for (var j = 0; j < this.state.filterSelection.length; j++) {
				if (i != j) {
					if (this.isAncestor(this.state.filterSelection[i], this.state.filterSelection[j])) {
						indicesOfElementsWithAncestorsInSelection.push(j);
					}
				}
			}
		}
		indicesOfElementsWithAncestorsInSelection.sort();
		for (var i = 0; i < indicesOfElementsWithAncestorsInSelection.length; i++) {
			this.state.filterSelection.splice(indicesOfElementsWithAncestorsInSelection[i] - i, 1);
		}

		//if no elements are selected, no group can be created
		if (this.state.filterSelection.length < 1) {
			return;
		}		

		//find lca
		let lca = this.findLowestCommonAncestor(this.state.filterSelection);

		//just relevant in case of filterSelection.length == 1, because in this case the lca is basic
		if (this.state.filterSelection.length == 1) {
			lca = lca.parent;
		}

		//find lowest index of filterSelection to insert group there, note we cannot use euler because euler only applies to basic elements
		let minIndex = lca.children.length - 1;
		for (var i = 0; i < this.state.filterSelection.length; i++) {
			let current = this.state.filterSelection[i];
			while (current.parent != lca) {
				current = current.parent;
			}
			minIndex = Math.min(minIndex, current.parent.children.indexOf(current));
		}

		//move operators
		let operatorIndicesToMove = [];
		for (var i = 0; i < this.state.filterSelection.length; i++) {
			operatorIndicesToMove = operatorIndicesToMove.concat(this.getAllEulerValues(this.state.filterSelection[i]));
		}

		//if leftmost element was chosen, no operator needs to be moved for this element
		if (operatorIndicesToMove[0] == -1) {
			operatorIndicesToMove.splice(0, 1);
		}

		console.log(this.state.ops);
		console.log(operatorIndicesToMove);
		
		let clonedOps = [...this.state.ops];
		for (var i = 0; i < operatorIndicesToMove.length; i++) {
			this.state.ops.splice(operatorIndicesToMove[i] - i, 1);
		}

		console.log(this.state.ops);

		for (var i = 0; i < operatorIndicesToMove.length; i++) {
			let position = Math.max(operatorIndicesToMove[0] + i, 0);
			this.state.ops.splice(position, 0, clonedOps[operatorIndicesToMove[i]]);
		}

		console.log(this.state.ops);

		//declare new element (a group holding all selected items)
		let newElement = {id: Math.random(), parent: lca, basic: false, children: []};

		//move selected elements to new element
		let clones = [];
		for (var i = 0; i < this.state.filterSelection.length; i++) {
			let current = this.state.filterSelection[i];
			let clone = {...current};
			clones.push(clone);
			current.parent = newElement;
			newElement.children.push(current);			
		}

		//move new element to lca
		lca.children.splice(minIndex, 0, newElement);

		//delete children from lca
		for (var i = 0; i < clones.length; i++) {
			let currentClone = clones[i];
			let currentReal = this.state.filterSelection[i];
			let index = currentClone.parent.children.indexOf(currentReal);
			if(index != -1) {
				currentClone.parent.children.splice(index, 1);
			}
		}

		//clean up
		this.removeEmptyGroups(this.state.filter);
		this.generateTextview();

		console.log(this.state.filter);

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: this.state.filter, filterSelection: [], order: prevState.order }));    
	}

	/**
	 * removes all empty groups
	 */
	removeEmptyGroups(filter) {
		if (filter.basic) {
			return;
		}

		for (var i = 0; i < filter.children.length; i++) {
			if (!this.hasAtLeastOneLeaf(filter.children[i])) {
				filter.children.splice(i, 1);
				i--;
			}
		}

		for (var i = 0; i < filter.children.length; i++) {
			this.removeEmptyGroups(filter.children[i]);
		}
	}

	/**
	 * checks if there is at least one basic in a group. To remove structures like this (())
	 */
	hasAtLeastOneLeaf(filter) {
		if (filter.basic) {
			return true;
		}

		let hasLeafs = false;
		for (var i = 0; i < filter.children.length; i++) {
			hasLeafs = hasLeafs || this.hasAtLeastOneLeaf(filter.children[i]);

			if (hasLeafs) {
				return true;
			}
		}

		return hasLeafs;
	}

	/**
	 * removes selected groups or elements if basic 
	 */
	removeGroup() {
		if (this.state.filterSelection.length <= 0) {
			return;
		}

		this.computeEulerTour();

		for (var i = 0; i < this.state.filterSelection.length; i++) {
			//take all children and move to parent's children
			let currentElement = this.state.filterSelection[i];

			if (currentElement.parent != null) {
				if (currentElement.basic) {
					//delete the basic element itself
					this.state.ops.splice(currentElement.euler - 1, 1);
					let index = currentElement.parent.children.indexOf(currentElement);
					currentElement.parent.children.splice(index, 1);
				} else {
					//remove self
					let index = currentElement.parent.children.indexOf(currentElement);
					if(index !== -1) {
						currentElement.parent.children.splice(index, 1);
					}

					//move children of group to groups parent
					for (var j = 0; j < currentElement.children.length; j++) {
						let currentChild = currentElement.children[j];
						currentChild.parent = currentElement.parent;
						currentElement.parent.children.splice(index + j, 0, currentChild);
					}
				}
			}
		}

		this.removeEmptyGroups(this.state.filter);

		this.generateTextview();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: this.state.ops, filter: this.state.filter, filterSelection: [], order: prevState.order }));    
	}

	/**
	 * returns the filter as html
	 */
	showFilter() {
		this.opCount = 0;
		return this.showFilterRecursive(this.state.filter);
	}

	/**
	 * prints the filter as html
	 */
	showFilterRecursive(filter) {
		if (filter.basic) {
			this.opCount++;

			if (filter.comparator == "IN") {
				let fullRange = [];
				let description = this.getByName(filter.name);
				console.log(description);
				if (description != null && description.discreetvalues != null) {
					fullRange = [...description.discreetvalues];
					if (filter.name == "Variant quality") {
						fullRange = ["PASS"];
					}
				}

				for (var i = 0; i < filter.value.length; i++) {
					if (!fullRange.includes(filter.value[i])) {
						fullRange.push(filter.value[i]);
					}
				}

				return <span draggable="true" onDragStart={(e) => this.drag(filter, e)} onClick={(e) => {this.toggleFilterSelection(filter, e)}} className={"basic " + (this.state.filterSelection.includes(filter) ? "selectedFilter" : "")}>
						<div className="emumList">
							{filter.name}
							{
								fullRange.map((item, index) => {
									return <div className="enumItem"><input onChange={(e) => {this.changeFilterEnum(filter, item); this.toggleFilterSelection(filter, e)}} checked={filter.value.includes(item)} type="checkbox" /> {item == null ? (<i>empty</i>) : item}</div>
								})
							}
						</div>
					</span>;									
			} else {
				return <span draggable="true" onDragStart={(e) => this.drag(filter, e)} onClick={(e) => {this.toggleFilterSelection(filter, e)}} className={"basic " + (filter.comparator == "=" ? "lightBasic " : "") + (this.state.filterSelection.includes(filter) ? "selectedFilter" : "")}>
					{filter.name + " "}
					<span>
						<span onClick={(e) => {if (filter.comparator != "=") {this.changeFilterComparator(filter);}}} className="comparator">{filter.comparator + " "}</span>
						<input onClick={(e) => this.toggleFilterSelection(null, e)} className={"smallInput " + (filter.name == "Panel" ? "wide " : "")} onChange={(e) => this.changeFilterValue(filter, e)} value={filter.value[0]}/>
					</span>
				</span>;
			}
		}

		let inner = <span>
				{
					filter.children.map((item, index) => {
						if (index == 0) {
							return this.showFilterRecursive(item);
						} else {
							let opIndex = this.opCount - 1;
							return <span>
									<span onClick={(e) => {this.toggleConnector(opIndex, e)}} className="connector">{this.state.ops[opIndex]}</span>
									{this.showFilterRecursive(item)}
								</span>;
						}
					})
				}</span>;

		if (filter.parent == null) {
			return inner;
		}

		return <span draggable="true" onClick={(e) => {this.toggleFilterSelection(filter, e)}} onDragStart={(e) => this.drag(filter, e)} onDrop={(e) => this.drop(filter, e)} onDragOver={(e) => this.allowDrop(filter, e)} className={"groupingBox " + (this.state.filterSelection.includes(filter) ? "selectedFilter" : "")}>
				{inner}
			</span>;
	}

  /**
   * adds an item to the filter
   */
	addToFilter(selected) {
		if (selected == null) {
			selected = this.state.selected;
		}

		let element = {
			id: "" + Math.random(),
			parent: this.state.filter,
			basic: true,
			name: selected.name,
		};

		console.log(selected);

		if (selected.discreetvalues != null) {
			element.comparator = "IN";
			element.value = [];
		} else {
			let defaultValue = 1;
			if (typeof selected.sample !== 'undefined' && selected.sample != null) {
				defaultValue = Number(selected.sample);
			}			
			element.comparator = "<";
			if (typeof selected.samplecomparator !== 'undefined' && selected.samplecomparator != null) {
				element.comparator = selected.samplecomparator;
			}
			element.value = [defaultValue];
		}

		this.state.filter.children.push(element);
		if (this.state.filter.children.length > 1) {
			let op = "AND";// + (this.state.filter.children.length - 1);
			this.state.ops.push(op);
		}

		this.generateTextview();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: this.state.ops, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

	addOrderToFilter = (order) => {
		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: order}));
		this.generateTextview();
	}

	addGenotypesToFilter = (genotypesToAdd, name) => {
	    let newGenotypes = [];

	    if (this.genotypes != null) {
		    for (var i = 0; i < this.genotypes.length; i++) {
		      if (this.genotypes[i].length > 0 && this.genotypes[i][0].name != name) {
		        newGenotypes.push(this.genotypes[i]);
		      }
		    }
		  }
	    
	    newGenotypes.push(genotypesToAdd);
		this.genotypes = newGenotypes;

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
		this.generateTextview();

		console.log(this.genotypes);
	}

	addPanelToFilter = (name) => {
		let element = {
			id: "" + Math.random(),
			basic: true,
			name: "Panel",
			comparator: "=",
			value: [name]
		};

		this.addGeneOrPanelToFilter(element);
	}

	addGeneToFilter = (symbol) => {
		let element = {
			id: "" + Math.random(),
			basic: true,
			name: "Gene",
			comparator: "=",
			value: [symbol]
		};

		this.addGeneOrPanelToFilter(element);
	} 

	addGeneOrPanelToFilter(element) {
		//if there is a group that only contains panels and genes, add it to that
		//if there is no group create one

		let panelGroup = null;

		//todo make this better
		if (this.state.filter.children.length > 0) {
			if (!this.state.filter.children[0].basic) {
				if (this.state.filter.children[0].children.length > 0) {
					if (this.state.filter.children[0].children[0].basic && this.state.filter.children[0].children[0].comparator == "=") {
						panelGroup = this.state.filter.children[0];
					}
				}
			}
		}		

		if (this.state.filter.children.length >= 1) {
			let op = "OR";
			if (panelGroup == null) {
				op = "AND";
			}
			if (this.state.ops.length == 0) {
				this.state.ops.push(op);
			} else {
				this.state.ops.splice(0, 0, op);
			}
		}		

		if (panelGroup == null) {
			panelGroup = {id: Math.random(), parent: this.state.filter, basic: false, children: [element]};
			this.state.filter.children.splice(0, 0, panelGroup);
		} else {
			panelGroup.children.splice(0, 0, element);
		}

		element.parent = panelGroup;

		this.generateTextview();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: this.state.ops, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

  /**
   * called whenever the value of a basic filter changes
   */
	changeFilterValue(filter, e) {
		let value = e.target.value;
		filter.value = [value];

		this.generateTextview();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

  /**
   * called whenever a checkbox is clicked in a basic filter
   */
	changeFilterEnum(filter, item) {
		let index = filter.value.indexOf(item);
		if (index != -1) {
			filter.value.splice(index, 1);
		} else {
			filter.value.push(item);
		}

		this.generateTextview();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

  /**
   * called when the comparator (e. g. < ) is clicked and toggles it
   */
	changeFilterComparator(filter) {
		let comparators = ["<", ">"]; //, "≤", "≥"];
		let index = comparators.indexOf(filter.comparator);
		filter.comparator = comparators[(index + 1) % comparators.length];

		this.generateTextview();

		this.toggledOnce = true;

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
	}

  /**
   * called if the connector (e. g. AND) is clicked and toggles it
   */
	toggleConnector(position, e) {
		let connectors = ["AND", "OR"];
		let index = connectors.indexOf(this.state.ops[position]);
		this.state.ops[position] = connectors[(index + 1) % connectors.length];

		this.generateTextview();

		this.toggledOnce = true;
		e.stopPropagation();

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: this.state.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));		
	}

	drag(filter, e) {
		e.dataTransfer.setData("id", filter.id);
		e.stopPropagation();
		console.log("drag bais " + filter.basic);
	}

	allowDrop(filter, e) {
		console.log("may drop in " + filter.id);
	  e.preventDefault();
	  e.stopPropagation();
	}

	/**
	 * returns a filter by id
	 */
	getFilterById(filter, id) {
		if (filter.id == id) {
			return filter;
		}

		if (filter.basic) {
			return null;
		}

		for (var i = 0; i < filter.children.length; i++) {
			let result = this.getFilterById(filter.children[i], id);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	/**
	 * JS drag and drop drop function
	 */
	drop(filter, e) {
	  e.preventDefault();
	  e.stopPropagation();
	  let draggedFilter = this.getFilterById(this.state.filter, e.dataTransfer.getData("id"));
	  this.moveToGroup(draggedFilter, filter);
	}

	/**
	 * move an element (basic or group) from one group to another
	 */
	moveToGroup(dragged, group) {
		if (dragged.parent == group) {
			return;
		}

		this.computeEulerTour();
		let operatorIndicesToMove = this.getAllEulerValues(dragged);

		//if leftmost element was chosen, move the first non affected operator
		if (operatorIndicesToMove[0] == -1) {
			let firstToTheRight = operatorIndicesToMove[operatorIndicesToMove.length - 1] + 1;
			operatorIndicesToMove.splice(0, 1);
			operatorIndicesToMove.splice(0, 0, firstToTheRight);
		}

		let sortedOperatorIndicesToMove = [...operatorIndicesToMove];
		sortedOperatorIndicesToMove.sort();
		
		let clonedOps = [...this.state.ops];
		for (var i = 0; i < sortedOperatorIndicesToMove.length; i++) {
			this.state.ops.splice(sortedOperatorIndicesToMove[i] - i, 1);
		}

		let eulerValuesOfGroup = this.getAllEulerValues(group);
		let targetPosition = eulerValuesOfGroup[eulerValuesOfGroup.length - 1];

		if (targetPosition > Math.max(...operatorIndicesToMove)) {
			targetPosition -= operatorIndicesToMove.length;
		}

		for (var i = 0; i < operatorIndicesToMove.length; i++) {
			let position = Math.max(targetPosition + i + 1, 0);
			this.state.ops.splice(position, 0, clonedOps[operatorIndicesToMove[i]]);
		}


		//remove basic from current parent
		let index = dragged.parent.children.indexOf(dragged);
		if (index != -1) {
			dragged.parent.children.splice(index, 1);
		}		

		//add basic to group's children
		group.children.push(dragged);
		dragged.parent = group;

		this.removeEmptyGroups(this.state.filter);

		this.setState(prevState => ({selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: this.state.ops, filter: this.state.filter, filterSelection: prevState.filterSelection, order: prevState.order}));		
	}

	/**
	 * stores the current filter as a file
	 */
	download() {
		this.opCount = 0;
		let text = this.buildText(this.state.filter);

    const element = document.createElement("a");
    const file = new Blob([text], {type: 'text/plain'});
    element.href = URL.createObjectURL(file);

    let fileName = this.fileNameRef.current.value;
    if (fileName == "") {
    	fileName = "filter";
    }

    element.download = fileName + ".txt";
    document.body.appendChild(element);
    element.click();
  }

  changePage(diff) {
  	this.setState(prevState => ({page: prevState.page + diff, selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
  }

  changePageTo(page) {
  	this.setState(prevState => ({page: page, selected: prevState.selected, columns: prevState.columns, foundColumns: prevState.foundColumns, selectedIndex: prevState.selectedIndex, ops: prevState.ops, filter: prevState.filter, filterSelection: prevState.filterSelection, order: prevState.order}));
  }  

  /**
   * takes the current filter and converts it to an API readable format
   */
  getJSONFilter() {
  	this.opCount = 0;

  	let result = {offset: 0, order: [], expression: null};

  	if (this.state.filter.children.length > 0) {
  		result.expression = this.getJSONFilterRecursive(this.state.filter);
  	}

  	//add genotypes
  	let expressionIndcludingGenotypes = {basic: false, operators: [], children: []};
  	if (result.expression != null) {
  		expressionIndcludingGenotypes.children.push(result.expression);
  	}
  	if (this.genotypes != null) {
  		for (var i = 0; i < this.genotypes.length; i++) {
  			let values = [];
  			for (var j = 0; j < this.genotypes[i].length; j++) {
  				values.push(this.genotypes[i][j].value);
  			}
  			if (this.genotypes[i].length > 0) {
	  			expressionIndcludingGenotypes.children.push({basic: true, name: this.genotypes[i][0].name, comparator: "IN", values: values});
	  			expressionIndcludingGenotypes.operators.push("AND");
	  		}
  		}
  	}
  	result.expression = expressionIndcludingGenotypes;

  	//add orders
  	result.order = [];
  	for (var i = 0; i < this.state.order.length; i++) {
  		var filterByName = this.getByName(this.state.order[i].name);
  		if (filterByName != null) {
  			result.order.push({name: filterByName.id, ascending: this.state.order[i].ascending });
  		}
  	}

  	console.log(result.expression);

  	return result;
  }

  getJSONFilterRecursive(filter) {
  	if (filter.basic) {
  		var filterByName = this.getByName(filter.name);

  		var id = filter.name;
  		if (filterByName != null) {
  			id = filterByName.id;
  		}

  		this.opCount++;
  		if (filter.comparator == "IN") {
  			return {basic: true, name: id, comparator: "IN", values: filter.value};
  		} else if (filter.comparator == "=") {
  			if (filter.name.toLowerCase() == "gene") {
					return {basic: true, name: "info_csq_symbol", comparator: "IN", values: filter.value };
  			} else {
  				var symbolsInPanel = [];
  				for (var i = 0; i < this.props.panelIndex?.panelIndex?.length; i++) {
  					var currentPanel = this.props.panelIndex.panelIndex[i];
  					if (currentPanel.name.replaceAll(/[^a-zA-Z0-9\s]/g, "").replaceAll("  ", " ").replaceAll(" ", "_").toUpperCase() == filter.value[0]) {
  						symbolsInPanel = currentPanel.geneSymbols;
  						break;
  					}
  				}
  				return {basic: true, name: "info_csq_symbol", comparator: "IN", values: symbolsInPanel};
  			}
  		} else {
  			return {basic: true, name: id, comparator: filter.comparator, value: parseFloat(('' + filter.value[0]).replaceAll(",", "."))};
  		}
  	}

		let result = {basic: false, operators: [], children: []};
		for (var i = 0; i < filter.children.length; i++) {
			if (i > 0) {
				result.operators.push(this.state.ops[this.opCount - 1]);
			}
			result.children.push(this.getJSONFilterRecursive(filter.children[i]));
		}

		return result;
  }

	getColor(value){
		//value from 0 to 1
		let hue = ((1-value)*120).toString(10);
		return ["hsl(",hue,",100%,50%)"].join("");
	}    

	render() {
		let filterDetail = <div className="filterDetail">
				The variant filtering procedure is comprised of four steps:
				<ol>
					<li>Filter for variant pathogenicity</li>
					<li>Filter for gene panels</li>
					<li>Filter for genotype</li>
					<li>Sort your result</li>
				</ol>
				Click on one item in the menu on the left or search for specific variant properties. A detailed description of the selected property will appear.
				If you have decided to use the selected property, click on “Add to filter”.
				The corresponding icon will appear in the filter builder below. Choose your threshold.
				You can use AND or OR links to connect filtering criteria.
				Use “Create group for filtering criteria" to add parentheses around the selected statements.
				<br/>
				<br/>
				Here you can filter for variant pathogenicity and allele frequency. Ensemble IMPACT
				rating, SIFT and Polyphen pathogenicity prediction and SpliceAI predictions are available
				across the entire genome. Most scores (FatHMM, M-CAP, DANN, PrimateAI etc.) however
				are available exclusively for non-synonymous mutations, which are based on a database
				with 80 million entries. GnomAD, and 1000 Genomes phase 3 allele frequencies are available for
				filtering. Variants not present in those 2 datasets are treated as if their frequency were 0.
			</div>;

		if (this.state.columns == null) {
			filterDetail = <div className="filterDetail">
											Loading filterable columns
										</div>

		} else if (this.state.foundColumns.length == 0) {
			filterDetail = <div className="filterDetail">
											No columns found
										</div>

		} else if (this.state.selected != null) {
			console.log(this.state.selected);
			let sampleValue = this.state.selected.sample;
			if (this.state.selected.discreetvalues != null && this.state.selected.discreetvalues.length > 0) {
				sampleValue = this.state.selected.discreetvalues[0];
			}
			filterDetail = <div className="filterDetail"> 
												<div className="head flex-container">
													<div className="title">
														{this.state.selected.name}
													</div>
													<div className="adder">
														<button className="sec" onClick={(e) => this.addToFilter(this.state.selected)}>Add to filter</button>
													</div>
												</div>
												<div className="body overflowAuto">
													{this.state.selected.description}
												</div>
												<div>
													<span className="heatSplashBackground"><span className="heatSplash" style={{"background" : (this.getColor(this.state.selected.nonnullrows == 0 ? 1 : (1 - Math.pow(Math.log(this.state.selected.nonnullrows)/Math.log(this.state.selected.overallrows), 2))))}}></span></span> Available in <b>{Number(this.state.selected.nonnullrows).toLocaleString()}</b> out of <b>{Number(Math.max(this.state.selected.overallrows, this.state.selected.nonnullrows)).toLocaleString()}</b> isoforms. 
												</div>
												<hr/>
												<div className="foot flex-container">
													<div className="sample">
														<table>
															<tr>
																<td>Sample Value:</td><td><span className="range">{sampleValue}</span></td>
															</tr>
															<tr>
																<td>Range:</td>
																<td>
																	<span className="range">{this.state.selected.discreetvalues == null ? (this.state.selected.from + " to " + this.state.selected.to) : this.state.selected.discreetvalues.map((item) => {return (item != null ? item + " " : "")})}</span>
																</td>
															</tr>
														</table>
													</div>
													<div className="paper">
														<a target="_blank" href={this.state.selected.link}>{this.state.selected.link}</a>
													</div>
												</div>
											</div>;
		}

		let pageTitle = "";

		let numberOfPages = 4;
		if (this.state.page == 1) {
			pageTitle = "Select panels or genes";
		} else if (this.state.page == 2) {
			pageTitle = "Select filtering criteria";
		} else if (this.state.page == 3) {
			pageTitle = "Filter by genotype";			
		} else if (this.state.page == 4) {
			pageTitle = "Sort the data";
		}

		let filterSelector = "";
		
		if (this.state.page == 1) {
			filterSelector = <PanelSelector ref={this.panelSelectorRef} addPanelToFilter={this.addPanelToFilter} addGeneToFilter={this.addGeneToFilter} panelIndex={this.props.panelIndex} token={this.props.token} />;
		} else if (this.state.page == 2) {
			filterSelector = <div className="filterSelector flex-container">
				<div className="filterList">
					<div className="body">
						{this.state.foundColumns.map((item, index) => {
							return <div onClick={(e) => this.select(index)} className={"filterRow " + (this.state.selectedIndex == index ? "selected" : "")}>
									{item.name} 
									<span onClick={(e) => {this.select(index); this.addToFilter(item);}} className={"filterRowType typeproperty"}>Add this property</span>
								</div>;
						})}
					</div>
				</div>
				{filterDetail}
			</div>;			
		} else if (this.state.page == 3) {
			filterSelector = <GenotypeSelector genotypes={this.genotypesSelected} meta={this.props.meta} addGenotypesToFilter={this.addGenotypesToFilter} />
		} else if (this.state.page == 4) {
			filterSelector = <OrderSelector order={this.state.order} addOrderToFilter={this.addOrderToFilter} filterColumns={this.state.columns} />
		}

		return  <div className="filterContainer">
							<div className="filterHeader">
								<span className="headingNumber">{this.state.page}</span>
								<span className="heading">{pageTitle}</span>
								<div className="rightAlign">
									<button onClick={(e) => this.changePage(-1)} className={"tert " + (this.state.page == 1 ? "hidden" : "")}><i class="bi bi-arrow-left"></i> Previous</button>
									<button onClick={(e) => this.changePage(1)} className={"sec " + (this.state.page == numberOfPages ? "hidden" : "")}>Next <i class="bi bi-arrow-right"></i></button>
									<button  onClick={(e) => this.props.apply(this.getJSONFilter())}  className={"sec " + (this.state.page != numberOfPages ? "hidden" : "")}>Apply Filter <i class="bi bi-play-circle"></i></button>
								</div>
							</div>
							{filterSelector}
							<div className="filterBuilder" onClick={(e) => this.unselectAllFilters(e)} onDrop={(e) => this.drop(this.state.filter, e)} onDragOver={(e) => this.allowDrop(this.state.filter, e)}>
								<button className={"sec " + (this.state.filter.children.length == 0 ? "hidden" : "") + " " + ((this.state.filterSelection == null || this.state.filterSelection.length == 0) ? "disabledButton" : "")} onClick={(e) => this.createGroup()}>Create group for selected filtering criteria</button> 
								<button className={"sec " + (this.state.filter.children.length == 0 ? "hidden" : "") + " " + ((this.state.filterSelection == null || this.state.filterSelection.length == 0) ? "disabledButton" : "")} onClick={(e) => this.removeGroup()}>Remove selected group or item</button>
								{((this.state.filterSelection == null || this.state.filterSelection.length == 0) && this.state.filter.children.length > 0) ? <span className="hint"><i class="bi bi-lightbulb-fill"></i> Click on the name of one or more filtering criteria below to select them for grouping or removal</span> : ""}
								{(this.state.filter == null || this.state.filter.children.length == 0) && this.state.page == 1 ? <span className="hint"><br/><i class="bi bi-lightbulb-fill"></i> Select one of the columns from the list and click 'Add to filter'<br/></span> : ""}
								{(this.state.filter == null || this.state.filter.children.length == 0) && this.state.page == 2 ? <span className="hint"><br/><i class="bi bi-lightbulb-fill"></i> Select one of the panels or genes from the list (or search for them) and click 'Add to filter'<br/></span> : ""}
								<button className={"clearFilter prim " + (((this.state.filter == null || this.state.filter.children.length == 0) && (this.genotypes == null || this.genotypes.length == 0)) ? "disabledButton" : "")} onClick={(e) => this.clearFilter()}>Clear filter</button>
								<br/>
								<br/>
								{this.showFilter()}
								{(this.state.filter != null && this.state.filter.children.length > 1 && !this.toggledOnce) ? <span className="hint"><br/><i class="bi bi-lightbulb-fill"></i> Click on the orange AND-Box to toggle it; click on the encircled '&lt;'-Symbol to toggle it</span> : ""}
							</div>
							<div className="filterTextview">
								<textarea ref={this.textareaRef} onChange={(e) => this.convertTextToFilterFromEvent(e)} />
							</div>
						</div>
	}
}

/**

old header with saveing and name
								<input ref={this.fileNameRef} defaultValue="MyFile" />
								<button onClick={(e) => this.download()} className="sec">Save</button>
								<button className="sec" onClick={(e) => this.inputFileRef.current.click()} >Upload</button>
								<input type="file" id="file" ref={this.inputFileRef} style={{display: 'none'}}/>
**/

export default Filter;
