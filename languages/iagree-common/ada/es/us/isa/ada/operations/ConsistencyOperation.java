/**
 * 	This file is part of ADA.
 *
 *     ADA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ADA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with ADA.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.us.isa.ada.operations;

import es.us.isa.ada.Operation;

/**
 * Checks if a document is consistent. In other words, this
 * operations checks the document has no errors that
 * invalidate it
 */
public interface ConsistencyOperation extends Operation {

	/**
	 * Returns true if the document is consistent, and 
	 * false in other case
	 * @return boolean consistent
	 */
	public boolean isConsistent();

}
