/*
 * This file is part of GraphStream.
 * 
 * GraphStream is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GraphStream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphStream.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2006 - 2009
 * 	Julien Baudry
 * 	Antoine Dutot
 * 	Yoann Pigné
 * 	Guilhelm Savin
 */
package org.miv.graphstream.tool.workbench.cli;

import org.miv.graphstream.graph.Element;
import org.miv.graphstream.graph.Edge;
import org.miv.graphstream.graph.Node;
import org.miv.graphstream.tool.workbench.Context;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Defines some node operations like 'add' or 'del'.
 * 
 * @author Guilhelm Savin
 *
 */
public class NodeOperations extends CLICommand
{
	private static final String PATTERN = "^(add|del) node \"(" + PATTERN_ID + ")\"(" + PATTERN_ATTRIBUTES + ")?$";
	
	static
	{
		CLI.registerCommand( new NodeOperations() );
	}
	
	protected NodeOperations()
	{
		super( PATTERN );
		
		attributes.put( "action", 1 );
		attributes.put( "id", 2 );
		attributes.put( "attributes", 3 );
		
		usage = "{add|del} node \"id\" [attributes]";
	}
	
	@Override
	public String execute(CLI cli, String cmd)
	{
		CLICommandResult ccr = result( cmd );
		if( ! ccr.isValid() ) 	return "bad command";
		if( cli.ctx == null ) 	return "no context";
		if( ! ccr.hasAttribute( "id" ) 
			|| ! ccr.hasAttribute( "action" ) ) 	return usage();
		
		String id = ccr.getAttribute( "id" );
		if( ccr.getAttribute( "action" ).equals( "add" ) )
		{
			Element e = cli.ctx.getGraph().addNode( id );
			cli.ctx.getHistory().registerAddNodeAction(e);
		}
		else
		{
			LinkedList<Edge> edges = new LinkedList<Edge>();
			Context ctx = cli.ctx;
			
			Node n = ctx.getGraph().getNode(id);
			Iterator<? extends Edge> ite = n.getEdgeIterator();
			
			while( ite.hasNext() )
				edges.add(ite.next());
			
			cli.ctx.getHistory().registerDelNodeAction(ctx.getGraph().getNode(id),edges);
			ctx.getGraph().removeNode( id );
		}
		
		if( ccr.hasAttribute( "attributes" ) )
			fillAttributes( cli.ctx.getGraph().getNode(id), ccr.getAttribute( "attributes" ) );
		
		return R_OK;
	}

}
