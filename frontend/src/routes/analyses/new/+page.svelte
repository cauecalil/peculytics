<script lang="ts">
	import { goto } from '$app/navigation';
	import { createAnalysis } from '$lib/api';
	import { notifyError } from '$lib/stores/notifications';
	import { getErrorMessage } from '$lib/utils';

	let title = $state('');
	let files = $state<File[]>([]);
	let fileTitles = $state<string[]>([]);
	let submitting = $state(false);
	let validationMessage = $state('');

	const deriveTitle = (filename: string) => filename.replace(/\.[^/.]+$/, '').replace(/[_-]+/g, ' ').trim();

	const handleFilesChange = (event: Event) => {
		const target = event.currentTarget as HTMLInputElement;
		const list = target.files ? Array.from(target.files) : [];
		files = list;
		fileTitles = list.map((file) => deriveTitle(file.name));
	};

	const updateFileTitle = (index: number, value: string) => {
		fileTitles = fileTitles.map((title, i) => (i === index ? value : title));
	};

	const submit = async () => {
		validationMessage = '';

		if (!title.trim()) {
			validationMessage = 'Analysis title is required.';
			return;
		}

		if (files.length === 0) {
			validationMessage = 'Select at least one CSV file.';
			return;
		}

		submitting = true;

		try {
			const normalizedTitles = fileTitles.map((value, index) => {
				const trimmed = value.trim();
				return trimmed.length > 0 ? trimmed : deriveTitle(files[index].name);
			});
			const response = await createAnalysis({
				title: title.trim(),
				files,
				fileTitles: normalizedTitles
			});
			await goto(`/analyses/${response.id}`);
		} catch (error) {
			notifyError(getErrorMessage(error, 'Unable to create analysis.'));
		} finally {
			submitting = false;
		}
	};
</script>

<svelte:head>
	<title>Peculytics - New Analysis</title>
</svelte:head>

<section class="space-y-10">
	<div class="space-y-3">
		<p class="section-kicker">New analysis</p>
		<h1 class="text-4xl sm:text-5xl">Create an analysis</h1>
		<p class="max-w-2xl text-base text-muted">
			Add a title, upload your CSV files, and the system will start processing transactions asynchronously.
		</p>
	</div>

	<div class="card animate-rise space-y-6 p-8">
		<div class="space-y-2">
			<label class="text-sm font-semibold" for="analysis-title">Analysis title</label>
			<input
				id="analysis-title"
				class="input-field"
				type="text"
				placeholder="March 2026 Expenses"
				bind:value={title}
			/>
		</div>

		<div class="space-y-2">
			<label class="text-sm font-semibold" for="analysis-files">CSV files</label>
			<input
				id="analysis-files"
				class="file-field"
				type="file"
				multiple
				accept=".csv,text/csv"
				onchange={handleFilesChange}
			/>
			<p class="text-xs text-muted">Up to 10 files. Each file must be 5 MB or smaller.</p>
		</div>

		{#if files.length > 0}
			<div class="space-y-3">
				<p class="text-sm font-semibold">File titles</p>
				<div class="grid gap-3">
					{#each files as file, index (file.name)}
						<div class="card-soft p-4">
							<p class="text-xs text-muted">{file.name}</p>
							<input
								class="input-field mt-2"
								type="text"
								value={fileTitles[index] || ''}
								oninput={(event) =>
									updateFileTitle(index, (event.currentTarget as HTMLInputElement).value)}
							/>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		{#if validationMessage}
			<div
				class="rounded-2xl border border-[color:var(--rose)]/40 bg-[color:var(--rose)]/10 px-4 py-3 text-sm text-[color:var(--rose)]"
			>
				{validationMessage}
			</div>
		{/if}

		<div class="flex flex-col gap-3 sm:flex-row sm:items-center">
			<button class="btn-primary" type="button" onclick={submit} disabled={submitting}>
				{submitting ? 'Creating...' : 'Create analysis'}
			</button>
			<p class="text-xs text-muted">
				The upload will return right away. Processing continues in the background.
			</p>
		</div>
	</div>
</section>
