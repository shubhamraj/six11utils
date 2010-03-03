#!/usr/bin/env python
"""Inference in graphical models by message passing.

$Revision: 1.15 $
$Date: 2005/03/11 22:44:17 $
$Author: vignaux $
"""

from numpy.numarray import *

def listLessOne(alist,k):
    return alist[0:k]+alist[k+1:len(alist)]


class Node:
    def __init__(self,name=''):
        self.name=name
            
    def __str__(self):
        return self.name

    def ding(self, toNode,outboundMsg, dingChain):
        # This simply alerts a neighbour (toNode) that it needs to
        # respond to a message (outboundMsg)
        # print 'node',self.name,'dings',toNode.name,'(',dingChain,') with',outboundMsg
        if dingChain < 10:
            dingChain = dingChain + 1
            toNode.respondToMessage(self,outboundMsg, dingChain)
        
    def respondToMessage(self,fromNode,inboundMsg, dingChain):
        # A node responds to an inbound message from fromNode, by
        # going through its other neighbours, recalculating messages
        # to them, and letting them know (via ding).
        k=self.edges.index(fromNode) # looks up the index
                                     # corresponding to fromNode
        self.msg[k]=inboundMsg
        receivers=listLessOne(self.edges,k)
        for r in receivers:
            i=self.edges.index(r)
            newMsg=self.calcMessage(i)
            self.ding(r,newMsg,dingChain)

        
class Multiplier(Node):
    def __init__(self,name='',vlen=1):
        Node.__init__(self,name)
        self.vlen=vlen
        self.edges=[]
        self.msg=[1]*len(self.edges)
        self.observed=False
            
    def __str__(self):
        return self.name

    def display(self):
        print '--------------------------------------------'
        print 'node',self.name
        if self.observed:
            print 'has been observed'
        for i in range(len(self.edges)):
            print 'from node: ',self.edges[i],' msg:',self.msg[i]
        p = product((self.msg ))
        # posterior is the product vector divided by Z
        # Z is simply the value that makes the sum-of-components equal to 1
        # (remember, a probability distribution has to have an area of 1)
        # so if the vector is (a b) but (a + b) is 0.2,  multiply each component
        # by 1/sum(a + b). Z = 1/sum(p).
        print 'posterior: ', p/max(0.00001, sum(p))


    def calcMessage(self,i):
        newMsg=product(listLessOne(self.msg,i))
        return newMsg

    def initialDing(self):
        # Multipliers that are terminal nodes need to ding their 
        # edge with a message consisting of all ones.
        print 'Initial ding from terminal node',self.name,'to',self.edges[0].name
        dingChain = 0
        self.ding(self.edges[0], ones((1,self.vlen),Float), dingChain)

class Summer(Node):
    def __init__(self,name='',edges=[],phi=[]):
        Node.__init__(self,name)
        self.edges=edges
        self.msg=[1]*len(self.edges)
        for i in range(len(self.edges)):
            neighbour=self.edges[i]
            self.msg[i] = ravel(ones((1,neighbour.vlen),Float)) # i.e. vector of ones
            
            # update the neighbour's edges and msgs
            neighbour.edges.append(self)
            neighbour.msg.append(self.msg[i])
        self.phi=phi
        # Check that dimensions of phi match the vlen's of variables.
        for i in range(len(edges)):
            if not((self.edges[i]).vlen == self.phi.shape[i]):
                print 'Ooops: shape of',self.name,'phi doesnt match its variables.'
                print 'Shape[',i,'] is',self.phi.shape[i]
                print 'vlen of edge',self.edges[i],'is',(self.edges[i]).vlen
                # ........AND WE SHOULD QUIT HERE, WITH ERROR MESSAGE...
                stderr.write('There is a mismatch between size of phi and of a message')
                

    def calcMessage(self,i):
        # We need to be able to leave out one dimension at will.
        # First, we rotate phi around so it's got the i-th axis first,
        # followed by the others in ascending order:
        nPhiDims = len(self.phi.shape)
        axesorder = [i] + listLessOne(range(nPhiDims),i)
        z=transpose(self.phi, axes=(axesorder))
        index=range(len(self.msg)-1)
        # Go through the other messages and "integrate them out." Each
        # time a variable is summed out like this the dimensionality
        # of z (i.e. phi) goes down by one. We're summing out the
        # right-most dimension of z. We go through msg in reverse
        # order so that the length of the msg and the *rightmost*
        # dimension of z match.
        index.reverse()
        othermsg = listLessOne(self.msg,i)
        for j in index:
            y=othermsg[j]*z
            z=transpose(sum(transpose(y),0))
        return z

    def initialDing(self):
        # Summers that are terminal nodes need to ding their (one)
        # edge with the "message" phi.
        print 'Initial ding from terminal node',self.name,'to',self.edges[0].name
        dingChain = 0
        self.ding(self.edges[0], self.phi, dingChain)

    def __str__(self):
        return self.name

    def display(self):
        print '--------------------------------------------'
        print self.name
        for i in range(len(self.edges)):
            print 'intray ',self.edges[i],' msg:',self.msg[i]
        print 'phi is:'
        print self.phi
#        q = array(1)
#        for z in self.msg:
#            q = multiply.outer(q,z)
#        q = q*self.phi
#        q = q / sum(q,len(self.edges)-1) # this normalises over the LAST INDEX...
#        print 'revised phi would appear to be...'
#        print q


class Observation(Summer):
    # Called if a variable is observed - obs is the resulting vector.
    # 1. Must ding all neighbours with obs.
    # 2. Must somehow disable incoming dings.
    # 3. The variable has to "know" its observed value.
    # ALL THESE will occur if self simply acquires a new terminal Summer,
    # with zeros-bar-one (which dings back self once).
    # For some reason it works okay here but not in burglar.py
    def __init__(self,observedNode,obs=[]):
        Summer.__init__(self,'OBS',[observedNode],obs)
        self.edges=[observedNode]
        self.initialDing()
        observedNode.observed = True # just for humans, not used algorithmically

    
if __name__ == '__main__':

    print 'Testing...'
    # This is the classic belief net example: burglar,
    # earthquake, alarm.

    # Define the variables first.
    b = Multiplier('burglar',2)
    e = Multiplier('earthquake',2)
    a = Multiplier('alarm',2)

    # Now define the factors, each with a matrix.
    B = Summer('B',[b],array([.2,.8]))
    E = Summer('E',[e],array([.4,.6]))
    A = Summer('A',[b,e,a],array([[[.9,.1],[.6,.4]],[[.3,.7],[.5,.5]]]))

    theMultipliers = [b,e,a]  # list of the variable nodes.
    theSummers = [B,E,A]      # list of the factor nodes.
    theNodes = theMultipliers + theSummers  # list of all the nodes.

    # Initialise all messages: every terminal node dings its neighbour.
    print '############ Initialising all messages'
    for i in theNodes:
        if len(i.edges) == 1: # having one edge makes it a terminal node.
            i.initialDing()

    for i in theMultipliers:
        i.display()
    A.display()

    print '############## observe',b.name
    Observation(b,array([0.0,1.0]))
    for i in theMultipliers:
        i.display()  # Notice e unchanged, despite message from A!
        # This wouldn't happen in general MRF graph, so it must be due
        # to normalisation...

    print '############## observe',a.name
    Observation(a,array([0.0,1.0]))
    for i in theMultipliers:
        i.display() # e is different: "explaining away"
    A.display()
        
