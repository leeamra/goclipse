/*******************************************************************************
 * Copyright (c) 2016 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.utilbox.concurrency;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractFuture2<RET> implements Future2<RET> {
	
	protected final CompletableResult<RET> completableResult = new CompletableResult<>();
	
	public AbstractFuture2() {
		super();
	}
	
	@Override
	public boolean isCancelled() {
		return completableResult.isCancelled();
	}
	
	@Override
	public boolean isTerminated() {
		return completableResult.isTerminated();
	}
	
	@Override
	public boolean tryCancel() {
		boolean wasCancelledNow = completableResult.setCancelledResult();
		if(wasCancelledNow) {
			handleCancellation();
		}
		return wasCancelledNow;
	}
	
	protected void handleCancellation() {
		// default: do nothing
	}
	
	@Override
	public void awaitTermination() throws InterruptedException {
		completableResult.awaitTermination();
	}
	
	@Override
	public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		completableResult.awaitTermination(timeout, unit);
	}
	
	@Override
	public RET getResult_forSuccessfulyCompleted() {
		assertTrue(isCompletedSuccessfully());
		return completableResult.getResult_forSuccessfulyCompleted();
	}
	
	/* -----------------  ----------------- */ 
	
	public Future<RET> asJavaUtilFuture() {
		return asFuture;
	}
	
	protected final Future<RET> asFuture = new Future<RET>() {
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return AbstractFuture2.this.tryCancel();
		}
		
		@Override
		public boolean isCancelled() {
			return AbstractFuture2.this.isCancelled();
		}
		
		@Override
		public boolean isDone() {
			return AbstractFuture2.this.isTerminated();
		}
		
		@Override
		public RET get() throws InterruptedException, ExecutionException {
			try {
				return awaitResult();
			} catch(Throwable e) {
				// Don't throw java.util.concurrent.CancellationException because it is a RuntimeException
				throw toExecutionException(e);
			}
		}
		@Override
		public RET get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			try {
				return awaitResult(timeout, unit);
			} catch(Throwable e) {
				// Don't throw java.util.concurrent.CancellationException because it is a RuntimeException
				throw toExecutionException(e);
			}
		}
		
		public ExecutionException toExecutionException(Throwable e) throws ExecutionException {
			return new ExecutionException(e);
		}
		
	};
	
	/* -----------------  ----------------- */
	
	public static class CompletedFuture<RET> extends AbstractFuture2<RET> {
		
		public CompletedFuture(RET result) {
			completableResult.setResult(result);
			assertTrue(isCompletedSuccessfully());
		}
		
		@Override
		protected void handleCancellation() {
			assertFail(); // Should not happen
		}
		
	}
	
}