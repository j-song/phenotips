/**
 * Class for organizing graphics for Person nodes.
 *
 * @class PersonVisuals
 * @extends AbstractPersonVisuals
 * @constructor
 * @param {Person} node The node for which the graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */


var PersonVisuals = Class.create(AbstractPersonVisuals, {
    
    initialize: function($super, node, x, y) {
        this._nameLabel = null;
        this._stillBirthLabel = null;
        this._ageLabel = null;
        this._childlessStatusLabel = null;
        this._disorderShapes = null;
        this._deadShape = null;
        this._unbornShape = null;
        this._childlessShape = null;
        this._isSelected = false;
        $super(node, x, y);
        this._hoverBox = new PersonHoverbox(node, x, y, this.getGenderSymbol());
    },

    /**
     * Draws the icon for this Person depending on the gender, life status and whether this Person is the proband.
     * Updates the disorder shapes.
     *
     * @method setGenderSymbol
     */
    setGenderSymbol: function($super) {
        if(this.getNode().getLifeStatus() == 'aborted') {
            this._genderSymbol && this._genderSymbol.remove();
            var side = PedigreeEditor.attributes.radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x = this.getX() - height,
                y = this.getY();
            var shape = editor.getPaper().path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
            shape.attr(PedigreeEditor.attributes.nodeShape);
            this._genderShape = shape;
            shape = editor.getPaper().set(shape.glow({width: 5, fill: true, opacity: 0.1}).transform(["t",3,3,"..."]), shape);

            if(this.getNode().isProband()) {
                shape.transform(["...s", 1.07]);
                shape.attr("stroke-width", 5);
            }

            if(this.getNode().getGender() == 'U') {
                this._genderSymbol = shape;
            }
            else {
                x = this.getX();
                y = this.getY() + PedigreeEditor.attributes.radius/1.4;
                var text = (this.getNode().getGender() == 'M') ? "Male" : "Female";
                var genderLabel = editor.getPaper().text(x, y, text).attr(PedigreeEditor.attributes.label);
                this._genderSymbol = editor.getPaper().set(shape, genderLabel);
            }
        }
        else {
            $super();
        }
        if(this.getNode().isProband()) {
            this.getGenderShape().transform(["...s", 1.07]);
            this.getGenderShape().attr("stroke-width", 5);
        }
        if(this.getHoverBox()) {
            this._genderSymbol.flatten().insertAfter(this.getBackElements().flatten());
        }
        else if (!this.getNode().isProband()) {
            this._genderSymbol.flatten().insertAfter(editor.getGraph().getProband().getGraphics().getAllGraphics().flatten());
        }
        this.updateDisorderShapes();
    },

    /**
     * Returns all graphical elements that are behind the gender symbol
     *
     * @method getBackElements
     * @return {Raphael.st}
     */
    getBackElements: function() {
        return this.getHoverBox().getBackElements().concat(editor.getPaper().set(this.getChildlessStatusLabel(), this.getChildlessShape()));
    },

    /**
     * Updates the name label for this Person
     *
     * @method updateNameLabel
     */
    updateNameLabel: function() {
        this._nameLabel && this._nameLabel.remove();
        var text =  "";
        this.getNode().getFirstName() && (text += this.getNode().getFirstName());
        this.getNode().getLastName() && (text += ' ' + this.getNode().getLastName());
        this._nameLabel && this._nameLabel.remove();
        if(text.strip() != '') {
            this._nameLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditor.attributes.radius, text);
            this.getNameLabel().attr({'font-size': 18, 'font-family': 'Cambria'});
        }
        else {
            this._nameLabel = null;
        }
        this.drawLabels();
    },

    /**
     * Returns the Person's name label
     *
     * @method getNameLabel
     * @return {Raphael.el}
     */
    getNameLabel: function() {
        return this._nameLabel;
    },

    /**
     * Returns colored blocks representing disorders
     *
     * @method getDisorderShapes
     * @return {Raphael.st} Set of disorder shapes
     */
    getDisorderShapes: function() {
        return this._disorderShapes;
    },

    /**
     * Displays the disorders currently registered for this node.
     *
     * @method updateDisorderShapes
     */
    updateDisorderShapes: function() {
        this._disorderShapes && this._disorderShapes.remove();
        var gradient = function(color, angle) {
            var hsb = Raphael.rgb2hsb(color),
                darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
            return angle +"-"+darker+":0-"+color+":100";
        };
        var disorderShapes = editor.getPaper().set(),
            person = this.getNode(),
            delta,
            color;
        if(this.getNode().getLifeStatus() == 'aborted') {

            var side = PedigreeEditor.attributes.radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x1 = this.getX() - height,
                y1 = this.getY();
            delta = (height * 2)/(person.getDisorders().length);

            for(var k = 0; k < person.getDisorders().length; k++) {
                var corner = [];
                var x2 = x1 + delta;
                var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                if (x1 < this.getX() && x2 >= this.getX()) {
                    corner = ["L", this.getX(), this.getY()-height];
                }
                var slice = editor.getPaper().path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']);
                color = gradient(editor.getLegend().getDisorderColor(this.getNode().getDisorders()[k].getDisorderID()), 70);
                disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5, stroke: 'none' }));
                x1 = x2;
                y1 = y2;
            }
            if(this.getNode().isProband()) {
                disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()-PedigreeEditor.attributes.radius]);
            }
        }
        else {
            var disorderAngle = (person.getDisorders().length == 0)?0:(360/person.getDisorders().length).round();
            delta = (360/(person.getDisorders().length))/2;

            for(var i = 0; i < person.getDisorders().length; i++) {
                color = gradient(editor.getLegend().getDisorderColor(person.getDisorders()[i].getDisorderID()), (i * disorderAngle)+delta);
                disorderShapes.push(sector(editor.getPaper(), this.getX(), this.getY(), PedigreeEditor.attributes.radius,
                    person.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
            }

            (disorderShapes.length < 2) ? disorderShapes.attr('stroke', 'none') : disorderShapes.attr({stroke: '#595959', 'stroke-width':.03});
            if(this.getNode().isProband()) {
                disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()]);
            }
        }
        this._disorderShapes = disorderShapes;
        this._disorderShapes.flatten().insertAfter(this.getGenderSymbol().flatten());
    },

    /**
     * Draws a line across the Person to display that he is dead (or aborted).
     *
     * @method drawDeadShape
     */
    drawDeadShape: function() {
        var x, y;
        if(this.getNode().getLifeStatus() == 'aborted') {
            var side = PedigreeEditor.attributes.radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2);
            x = this.getX() - height/1.5;
            y = this.getY() + height/3;
            this._deadShape = editor.getPaper().path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
            this._deadShape.attr("stroke-width", 3);
        }
        else {
            x = this.getX();
            y = this.getY();
            var
                x1 = x - (10/8) * PedigreeEditor.attributes.radius,
                y1 = y + (10/8) * PedigreeEditor.attributes.radius,
                x2 = x + (10/8) * PedigreeEditor.attributes.radius,
                y2 = y - (10/8) * PedigreeEditor.attributes.radius;
            this._deadShape = editor.getPaper().path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", 3);
        }
        this._deadShape.insertAfter(this.getHoverBox().getFrontElements().flatten());
    },

    /**
     * Returns the line drawn across a dead Person's icon
     *
     * @method getDeadShape
     * @return {Raphael.st}
     */
    getDeadShape: function() {
        return this._deadShape;
    },

    /**
     * Returns this Person's age label
     *
     * @method getAgeLabel
     * @return {Raphael.el}
     */
    getAgeLabel: function() {
        return this._ageLabel;
    },

    /**
     * Updates the age label for this Person
     *
     * @method updateAgeLabel
     */
    updateAgeLabel: function() {
        var text,
            person = this.getNode();
        if (person.isFetus()) {
            var date = person.getGestationAge();
            text = (date) ? date + " weeks" : null;
        }
        else if(person.getLifeStatus() == 'alive') {
            person.getBirthDate() && (text = getAge(person.getBirthDate(), null));
        }
        else {
            var prefix = (person.getConceptionDate()) ? '' : "d. ";
            if(person.getDeathDate() && person.getBirthDate()) {
                text = prefix + getAge(person.getBirthDate(), person.getDeathDate());
            }
            else if(person.getDeathDate() && person.getConceptionDate()) {
                text = prefix + getAge(person.getConceptionDate(), person.getDeathDate());
            }
            else if (person.getDeathDate()) {
                text = prefix + person.getDeathDate().getFullYear();
            }
            else if(person.getBirthDate()) {
                text = person.getBirthDate().getFullYear() + " - ?";
            }
        }
        this.getAgeLabel() && this.getAgeLabel().remove();
        this._ageLabel = text ? editor.getPaper().text(this.getX(), this.getY(), text) : null;
        this.drawLabels();
    },

    /**
     * Returns the shape marking a Person's 'unborn' life-status
     *
     * @method getUnbornShape
     * @return {Raphael.el}
     */
    getUnbornShape: function() {
        return this._unbornShape;
    },

    /**
     * Draws a "P" on top of the node to display this Person's 'unborn' life-status
     *
     * @method drawUnbornShape
     */
    drawUnbornShape: function() {
        this._unbornShape && this._unbornShape.remove();
        if(this.getNode().getLifeStatus() == 'unborn') {
            this._unbornShape = editor.getPaper().text(this.getX(), this.getY(), "P").attr(PedigreeEditor.attributes.unbornShape);
            this._unbornShape.insertBefore(this.getHoverBox().getFrontElements());
        }
    },

    /**
     * Returns this Person's stillbirth label
     *
     * @method getSBLabel
     * @return {Raphael.el}
     */
    getSBLabel: function() {
        return this._stillBirthLabel;
    },

    /**
     * Updates the stillbirth label for this Person
     *
     * @method updateSBLabel
     */
    updateSBLabel: function() {
        var SBLabel;
        this.getNode().getLifeStatus() == 'stillborn' && (SBLabel = editor.getPaper().text(this.getX(), this.getY(), "SB"));
        this.getSBLabel() && this.getSBLabel().remove();
        this._stillBirthLabel = SBLabel;
        this.drawLabels();
    },

    /**
     * Displays the correct graphics to represent the current life status for this Person.
     *
     * @method updateLifeStatusShapes
     */
    updateLifeStatusShapes: function() {
        var status = this.getNode().getLifeStatus();
        this.getDeadShape() && this.getDeadShape().remove();
        this.getUnbornShape() && this.getUnbornShape().remove();
        this.setGenderSymbol();
        (!this.getNode().getPartnerships()[0]) && !this.getNode().getChildlessStatus() && this.getHoverBox().unhideChildHandle();
        this.getHoverBox().unhidePartnerHandles();

        if(status == 'deceased'){
            this.drawDeadShape();
        }
        else if(status == 'stillborn') {
            this.getHoverBox().hidePartnerHandles();
            this.drawDeadShape();
        }
        else if(status == 'aborted') {
            this.drawDeadShape();
            this.getHoverBox().hidePartnerHandles();
        }
        else if(status == 'unborn') {
            this.drawUnbornShape();
        }
        if(this.getNode().isFetus()){
            this.getHoverBox().hidePartnerHandles();
            this.getHoverBox().hideChildHandle();
        }
    },

    /**
     * Marks this node as hovered, and moves the labels out of the way
     *
     * @method setSelected
     */
    setSelected: function($super, isSelected) {
        $super(isSelected);
        if(isSelected) {
            this.shiftLabels();
        }
        else {
            this.unshiftLabels();
        }
    },

    /**
     * Moves the labels down to make space for the hoverbox
     *
     * @method shiftLabels
     */
    shiftLabels: function() {
        if(!this.getChildlessStatusLabel()) {
            var labels = this.getLabels();
            for(var i = 0; i<labels.length; i++) {
                labels[i].stop().animate({"y": labels[i].oy + PedigreeEditor.attributes.radius/1.5}, 200,">");
            }
        }
    },

    /**
     * Animates the labels of this node to their original position under the node
     *
     * @method unshiftLabels
     */
    unshiftLabels: function() {
        if(!this.getChildlessStatusLabel()) {
            var labels = this.getLabels();
            for(var i = 0; i<labels.length; i++) {
                labels[i].stop().animate({"y": labels[i].oy}, 200,">");
            }
        }
    },

    /**
     * Returns set of labels for this Person
     *
     * @method getLabels
     * @return {Raphael.st}
     */
    getLabels: function() {
        var labels = editor.getPaper().set();
        this.getSBLabel() && labels.push(this.getSBLabel());
        this.getNameLabel() && labels.push(this.getNameLabel());
        this.getAgeLabel() && labels.push(this.getAgeLabel());
        return labels;
    },

    /**
     * Displays all the appropriate labels for this Person in the correct layering order
     *
     * @method drawLabels
     */
    drawLabels: function() {
        var labels = this.getLabels(),
            selectionOffset = (this.isSelected() && !this.getChildlessStatusLabel()) ? PedigreeEditor.attributes.radius/1.5 : 0,
            childlessOffset = (this.getChildlessStatusLabel()) ? PedigreeEditor.attributes.radius/2 : 0,
            startY = this.getY() + PedigreeEditor.attributes.radius * 1.7 + selectionOffset + childlessOffset;
        for (var i = 0; i < labels.length; i++) {
            labels[i].attr("y", startY + 11);
            labels[i].attr(PedigreeEditor.attributes.label);
            labels[i].oy = (labels[i].attr("y") - selectionOffset);
            startY = labels[i].getBBox().y2;
        }
        labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
    },

    /**
     * Returns set with the gender icon, disorder shapes and life status shapes.
     *
     * @method getShapes
     * @return {Raphael.st}
     */
    getShapes: function($super) {
        var lifeStatusShapes = editor.getPaper().set();
        this.getUnbornShape() && lifeStatusShapes.push(this.getUnbornShape());
        this.getChildlessShape() && lifeStatusShapes.push(this.getChildlessShape());
        this.getChildlessStatusLabel() && lifeStatusShapes.push(this.getChildlessStatusLabel());
        this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
        return $super().concat(editor.getPaper().set(this.getDisorderShapes(), lifeStatusShapes));
    },

    /**
     * Returns all the graphics and labels associated with this Person.
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function($super) {
        return $super().push(this.getHoverBox().getBackElements(), this.getLabels(), this.getHoverBox().getFrontElements());
    },

    /**
     * Changes the position of the node to (x,y)
     *
     * @method setPos
     * @param [$super]
     * @param {Number} x the x coordinate on the canvas
     * @param {Number} y the y coordinate on the canvas
     * @param {Boolean} animate set to true if you want to animate the transition
     * @param {Function} callback a function that will be called at the end of the animation
     */
    setPos: function($super, x, y, animate, callback) {
    var funct;
        if(animate) {
            var me = this;
            this.getHoverBox().disable();
            funct = function () {
                me.getHoverBox().enable();
                callback && callback();
            };
        }
        $super(x, y, animate, funct);
    },

    /**
     * [Helper for setPos] Saves the x and y values as current coordinates and updates connections with the new position
     *
     * @method _updatePositionData
     * @param [$super]
     * @param {Number} x The new x coordinate
     * @param {Number} y The new y coordinate
     * @private
     */
    _updatePositionData: function($super, x, y) {
        this.getHoverBox().enable.bind(this.getHoverBox());
        $super(x, y)
    }
});

//ATTACHES CHILDLESS BEHAVIOR METHODS
PersonVisuals.addMethods(ChildlessBehaviorVisuals);