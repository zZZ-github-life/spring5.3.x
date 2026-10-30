/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;

/**
 * 简单的秒表，可用于对多个任务计时，并公开总运行时间以及每个命名任务的运行时间。
 *
 * <p>隐藏了 {@link System#nanoTime()} 的使用，从而提高应用代码的可读性并减少出现计算错误的可能性。
 *
 * <p>请注意，此对象并非为线程安全而设计，也不使用同步。
 *
 * <p>此类通常用于在概念验证工作和开发期间验证性能，而不是作为生产应用的一部分。
 *
 * <p>从 Spring Framework 5.2 开始，运行时间以纳秒为单位进行跟踪和报告。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since May 2, 2001
 */
public class StopWatch {

	/**
	 * 此 {@code StopWatch} 的标识符。
	 * <p>当存在多个秒表的输出并需要在日志或控制台输出中区分它们时，这很方便。
	 */
	private final String id;

	private boolean keepTaskList = true;

	private final List<TaskInfo> taskList = new ArrayList<>(1);

	/** 当前任务的开始时间。 */
	private long startTimeNanos;

	/** 当前任务的名称。 */
	@Nullable
	private String currentTaskName;

	@Nullable
	private TaskInfo lastTaskInfo;

	private int taskCount;

	/** 总运行时间。 */
	private long totalTimeNanos;


	/**
	 * 构造一个新的 {@code StopWatch}。
	 * <p>不会启动任何任务。
	 */
	public StopWatch() {
		this("");
	}

	/**
	 * 使用给定 ID 构造一个新的 {@code StopWatch}。
	 * <p>当存在多个秒表的输出并需要在它们之间进行区分时，该 ID 很方便。
	 * <p>不会启动任何任务。
	 * @param id 该秒表的标识符
	 */
	public StopWatch(String id) {
		this.id = id;
	}


	/**
	 * 获取此 {@code StopWatch} 的 ID，即在构造时指定的 ID。
	 * @return ID（默认为空 String）
	 * @since 4.2.2
	 * @see #StopWatch(String)
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 配置是否随时间构建 {@link TaskInfo} 数组。
	 * <p>当将 {@code StopWatch} 用于数百万个时间间隔时，请将其设置为 {@code false}；否则，
	 * {@code TaskInfo} 结构将消耗过多内存。
	 * <p>默认值为 {@code true}。
	 */
	public void setKeepTaskList(boolean keepTaskList) {
		this.keepTaskList = keepTaskList;
	}


	/**
	 * 启动一个未命名任务。
	 * <p>如果未先调用此方法就调用 {@link #stop()} 或计时方法，则结果是不确定的。
	 * @see #start(String)
	 * @see #stop()
	 */
	public void start() throws IllegalStateException {
		start("");
	}

	/**
	 * 启动一个命名任务。
	 * <p>如果未先调用此方法就调用 {@link #stop()} 或计时方法，则结果是不确定的。
	 * @param taskName 要启动的任务名称
	 * @see #start()
	 * @see #stop()
	 */
	public void start(String taskName) throws IllegalStateException {
		if (this.currentTaskName != null) {
			throw new IllegalStateException("Can't start StopWatch: it's already running");
		}
		this.currentTaskName = taskName;
		this.startTimeNanos = System.nanoTime();
	}

	/**
	 * 停止当前任务。
	 * <p>如果未调用至少一对 {@code start()} / {@code stop()} 方法就调用计时方法，则结果是不确定的。
	 * @see #start()
	 * @see #start(String)
	 */
	public void stop() throws IllegalStateException {
		if (this.currentTaskName == null) {
			throw new IllegalStateException("Can't stop StopWatch: it's not running");
		}
		long lastTime = System.nanoTime() - this.startTimeNanos;
		this.totalTimeNanos += lastTime;
		this.lastTaskInfo = new TaskInfo(this.currentTaskName, lastTime);
		if (this.keepTaskList) {
			this.taskList.add(this.lastTaskInfo);
		}
		++this.taskCount;
		this.currentTaskName = null;
	}

	/**
	 * 确定此 {@code StopWatch} 当前是否正在运行。
	 * @see #currentTaskName()
	 */
	public boolean isRunning() {
		return (this.currentTaskName != null);
	}

	/**
	 * 获取当前正在运行的任务的名称（如果有）。
	 * @since 4.2.2
	 * @see #isRunning()
	 */
	@Nullable
	public String currentTaskName() {
		return this.currentTaskName;
	}

	/**
	 * 获取最后一个任务所花费的时间，单位为纳秒。
	 * @since 5.2
	 * @see #getLastTaskTimeMillis()
	 */
	public long getLastTaskTimeNanos() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task interval");
		}
		return this.lastTaskInfo.getTimeNanos();
	}

	/**
	 * 获取最后一个任务所花费的时间，单位为毫秒。
	 * @see #getLastTaskTimeNanos()
	 */
	public long getLastTaskTimeMillis() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task interval");
		}
		return this.lastTaskInfo.getTimeMillis();
	}

	/**
	 * 获取最后一个任务的名称。
	 */
	public String getLastTaskName() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task name");
		}
		return this.lastTaskInfo.getTaskName();
	}

	/**
	 * 获取最后一个任务，以 {@link TaskInfo} 对象的形式返回。
	 */
	public TaskInfo getLastTaskInfo() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task info");
		}
		return this.lastTaskInfo;
	}


	/**
	 * 获取所有任务的纳秒总时间。
	 * @since 5.2
	 * @see #getTotalTimeMillis()
	 * @see #getTotalTimeSeconds()
	 */
	public long getTotalTimeNanos() {
		return this.totalTimeNanos;
	}

	/**
	 * 获取所有任务的毫秒总时间。
	 * @see #getTotalTimeNanos()
	 * @see #getTotalTimeSeconds()
	 */
	public long getTotalTimeMillis() {
		return nanosToMillis(this.totalTimeNanos);
	}

	/**
	 * 获取所有任务的秒总时间。
	 * @see #getTotalTimeNanos()
	 * @see #getTotalTimeMillis()
	 */
	public double getTotalTimeSeconds() {
		return nanosToSeconds(this.totalTimeNanos);
	}

	/**
	 * 获取已计时的任务数。
	 */
	public int getTaskCount() {
		return this.taskCount;
	}

	/**
	 * 获取已执行任务的数据数组。
	 */
	public TaskInfo[] getTaskInfo() {
		if (!this.keepTaskList) {
			throw new UnsupportedOperationException("Task info is not being kept!");
		}
		return this.taskList.toArray(new TaskInfo[0]);
	}


	/**
	 * 获取总运行时间的简短描述。
	 */
	public String shortSummary() {
		return "StopWatch '" + getId() + "': running time = " + getTotalTimeNanos() + " ns";
	}

	/**
	 * 生成一个包含描述所有已执行任务的表格的字符串。
	 * <p>如需自定义报告，请调用 {@link #getTaskInfo()} 并直接使用任务信息。
	 */
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder(shortSummary());
		sb.append('\n');
		if (!this.keepTaskList) {
			sb.append("No task info kept");
		}
		else {
			sb.append("---------------------------------------------\n");
			sb.append("ns         %     Task name\n");
			sb.append("---------------------------------------------\n");
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setMinimumIntegerDigits(9);
			nf.setGroupingUsed(false);
			NumberFormat pf = NumberFormat.getPercentInstance();
			pf.setMinimumIntegerDigits(3);
			pf.setGroupingUsed(false);
			for (TaskInfo task : getTaskInfo()) {
				sb.append(nf.format(task.getTimeNanos())).append("  ");
				sb.append(pf.format((double) task.getTimeNanos() / getTotalTimeNanos())).append("  ");
				sb.append(task.getTaskName()).append('\n');
			}
		}
		return sb.toString();
	}

	/**
	 * 生成一个描述所有已执行任务的信息性字符串
	 * <p>如需自定义报告，请调用 {@link #getTaskInfo()} 并直接使用任务信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(shortSummary());
		if (this.keepTaskList) {
			for (TaskInfo task : getTaskInfo()) {
				sb.append("; [").append(task.getTaskName()).append("] took ").append(task.getTimeNanos()).append(" ns");
				long percent = Math.round(100.0 * task.getTimeNanos() / getTotalTimeNanos());
				sb.append(" = ").append(percent).append('%');
			}
		}
		else {
			sb.append("; no task info kept");
		}
		return sb.toString();
	}


	private static long nanosToMillis(long duration) {
		return TimeUnit.NANOSECONDS.toMillis(duration);
	}

	private static double nanosToSeconds(long duration) {
		return duration / 1_000_000_000.0;
	}


	/**
	 * 用于保存 {@code StopWatch} 内执行的一个任务相关数据的嵌套类。
	 */
	public static final class TaskInfo {

		private final String taskName;

		private final long timeNanos;

		TaskInfo(String taskName, long timeNanos) {
			this.taskName = taskName;
			this.timeNanos = timeNanos;
		}

		/**
		 * 获取此任务的名称。
		 */
		public String getTaskName() {
			return this.taskName;
		}

		/**
		 * 获取此任务所花费的时间，单位为纳秒。
		 * @since 5.2
		 * @see #getTimeMillis()
		 * @see #getTimeSeconds()
		 */
		public long getTimeNanos() {
			return this.timeNanos;
		}

		/**
		 * 获取此任务所花费的时间，单位为毫秒。
		 * @see #getTimeNanos()
		 * @see #getTimeSeconds()
		 */
		public long getTimeMillis() {
			return nanosToMillis(this.timeNanos);
		}

		/**
		 * 获取此任务所花费的时间，单位为秒。
		 * @see #getTimeMillis()
		 * @see #getTimeNanos()
		 */
		public double getTimeSeconds() {
			return nanosToSeconds(this.timeNanos);
		}

	}

}
