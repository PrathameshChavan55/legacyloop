# The pattern every feature follows

Copy this shape. Six people writing the same shape is what makes the code reviewable in a viva.

## Backend, one vertical slice

**Entity** — `entity/Job.java`

```java
@Entity
@Table(name = "jobs", indexes = @Index(name = "idx_job_status", columnList = "status"))
@Getter @Setter @NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)   // LAZY everywhere; fetch what you need with a join
    @JoinColumn(name = "company_id")
    private Company company;

    @CreationTimestamp
    private Instant createdAt;
}
```

**Repository** — `repository/JobRepository.java`

```java
public interface JobRepository extends JpaRepository<Job, Long> {

    // one query, not one per row: this is the N+1 fix the reviewer asks about
    @Query("select j from Job j join fetch j.company where j.status = :status")
    Page<Job> findOpen(@Param("status") JobStatus status, Pageable pageable);

    boolean existsByIdAndPostedByUserId(Long id, Long userId);
}
```

**DTOs** — `dto/JobDtos.java` (one file, several records)

```java
public final class JobDtos {

    public record CreateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank String description,
            @NotNull Long companyId) {}

    public record Summary(Long id, String title, String companyName, String postedByName) {}

    private JobDtos() {}
}
```

**Service** — `service/JobService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public PageResponse<JobDtos.Summary> listOpen(Pageable pageable) {
        return PageResponse.of(jobRepository.findOpen(JobStatus.OPEN, pageable).map(this::toSummary));
    }

    @Transactional
    public JobDtos.Summary create(JobDtos.CreateRequest request, AuthUser caller) {
        Job job = new Job();
        job.setTitle(request.title());
        log.info("job created by user {}", caller.userId());
        return toSummary(jobRepository.save(job));
    }

    private Job find(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Job not found"));
    }
}
```

**Controller** — `controller/JobController.java`

```java
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ApiResponse<PageResponse<JobDtos.Summary>> list(Pageable pageable) {
        return ApiResponse.ok(jobService.listOpen(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ALUMNI', 'INSTITUTION_STAFF', 'PLATFORM_ADMIN')")
    public ApiResponse<JobDtos.Summary> create(@Valid @RequestBody JobDtos.CreateRequest request,
                                               @AuthenticationPrincipal AuthUser caller) {
        return ApiResponse.ok(jobService.create(request, caller), "Job posted");
    }
}
```

You never write a try/catch for a "not found" or a validation failure — throw `ApiException` and
`GlobalExceptionHandler` in `common` turns it into the right status code and body.

## Frontend, one page

```jsx
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { Card, EmptyState, ErrorState, PageHeader, Spinner } from '../components/ui'

export default function Jobs() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => api.jobs.list(),
  })

  if (isLoading) return <Spinner label="Loading jobs" />
  if (error) return <ErrorState error={error} onRetry={refetch} />
  if (!data?.content?.length) return <EmptyState title="No jobs yet" message="Check back later." />

  return (
    <div className="space-y-3">
      <PageHeader title="Jobs" subtitle={`${data.totalElements} open`} />
      {data.content.map((job) => (
        <Card key={job.id} className="p-4">
          <h2 className="font-medium">{job.title}</h2>
          <p className="text-sm text-slate-500">{job.companyName}</p>
        </Card>
      ))}
    </div>
  )
}
```

Loading, error, empty, data — four states, every page. `Spinner`, `ErrorState`, `EmptyState`,
`PageHeader`, `Card`, `Button`, `Badge`, `Avatar`, `Modal`, `Pagination`, `Tabs` are all already in
`components/ui.jsx`; import them rather than writing your own.

## React 19 notes

The project runs React 19.2, so a few older patterns will not work if you paste them in:

- `ReactDOM.render` is gone — `main.jsx` already uses `createRoot`, do not change it
- `propTypes` and `defaultProps` on function components are ignored — use default parameters
- `forwardRef` is no longer needed for a `ref` prop, though it still works
- `useEffectEvent` and `<Activity>` are available if you want them; nothing here requires them
